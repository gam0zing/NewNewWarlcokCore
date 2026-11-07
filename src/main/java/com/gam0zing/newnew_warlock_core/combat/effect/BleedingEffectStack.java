package com.gam0zing.newnew_warlock_core.combat.effect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.mixin.LivingEntityEffectHooks;
import com.gam0zing.newnew_warlock_core.mixin.MobEffectInstanceStackAccess;

/**
 * 出血容器：直接继承子实例类型 {@link BleedingEffectInstance}。
 *
 * <p>容器不复制外层字段，而是重写原版 API 实时聚合子实例：</p>
 * <ul>
 *     <li>{@link #getAmplifier()}：所有子实例等级之和，只在赋值给 amplifier 时截断到原版上限
 *     （缓存保存完整合计值，内部不丢失等级信息）；</li>
 *     <li>{@link #getDuration()} / {@link #isInfiniteDuration()}：子实例时长最大值，任一无限即无限；</li>
 *     <li>{@link #isAmbient()} / {@link #isVisible()} / {@link #showIcon()}：取首个存活子实例的展示标志；</li>
 *     <li>原版 HUD、网络包、属性修饰符、到期判断、存档都通过上述 getter 读取，因此口径天然一致。</li>
 * </ul>
 *
 * <p><b>结算方式：合并成一次生效</b></p>
 * <ul>
 *     <li>整个容器在每个间隔只结算一次，等级取所有子实例等级之和，
 *     因此 2 份 I 级出血等于 1 份 II 级，而不是两次独立的 I 级结算；</li>
 *     <li>结算相位沿用原版语义：{@code 剩余时长 % 间隔 == 0}，剩余时长取子实例最大值；</li>
 *     <li>结算来源取首个存活子实例（合并后无法再区分每一份的来源）。</li>
 * </ul>
 *
 * <p><b>生命周期</b></p>
 * <ul>
 *     <li>每个子实例独立保存等级、时长与来源，到期只剔除该子实例；</li>
 *     <li>列表为空时 {@link #tick} 返回 false，由原版从 {@code activeEffects} 移除容器；</li>
 *     <li>客户端只保存聚合后的普通实例，不重建容器。</li>
 * </ul>
 *
 * <p><b>已知边界</b></p>
 * <ul>
 *     <li>子实例不持有容器引用；需要容器时按实体查询 {@code getActiveEffects()} 或直接转型；</li>
 *     <li>不复制入栈对象，默认所有权转移；受容器管理的效果在并入时统一转换成
 *     {@link BleedingEffectInstance}，普通实例也先转换再入栈；</li>
 *     <li>子实例列表暂不设上限，也不做同源合并；等级缓存只在子实例增删时刷新，不写入 NBT；</li>
 *     <li>不支持 {@code hiddenEffect}，原版 tick 的到期回调不被使用。</li>
 * </ul>
 */
public class BleedingEffectStack extends BleedingEffectInstance implements EffectStack {

    private final List<BleedingEffectInstance> children = new ArrayList<>();

    /** 子实例等级之和的缓存，仅在子实例增删时失效；不参与存档 */
    private int cachedLevel;
    private boolean levelDirty = true;

    public BleedingEffectStack(Holder<MobEffect> effect) {
        super(effect);
    }

    @Override
    public MobEffectInstance self() {
        return this;
    }

    @Override
    public boolean pushChild(MobEffectInstance child) {
        if (child instanceof EffectStack other) {
            if (other == this) {
                return false;
            }

            boolean changed = false;
            for (MobEffectInstance nested : new ArrayList<>(other.children())) {
                changed |= this.pushChild(nested);
            }
            return changed;
        }

        if (!child.getEffect().equals(this.getEffect())) {
            return false;
        }

        this.children.add(asTyped(child));
        this.levelDirty = true;
        return true;
    }

    @Override
    public List<MobEffectInstance> children() {
        return Collections.unmodifiableList(new ArrayList<MobEffectInstance>(this.children));
    }

    @Override
    public int getAmplifier() {
        int level = this.totalLevel();
        return level <= 0 ? 0 : Math.min(MobEffectInstance.MAX_AMPLIFIER, level - 1);
    }

    @Override
    public int totalLevel() {
        if (!this.levelDirty) {
            return this.cachedLevel;
        }

        int level = 0;
        for (BleedingEffectInstance child : this.children) {
            level += child.getAmplifier() + 1;
        }
        this.cachedLevel = level;
        this.levelDirty = false;
        return level;
    }

    @Override
    public int getDuration() {
        if (this.children.isEmpty()) {
            return 0;
        }

        int max = 0;
        for (BleedingEffectInstance child : this.children) {
            if (child.isInfiniteDuration()) {
                return MobEffectInstance.INFINITE_DURATION;
            }
            max = Math.max(max, child.getDuration());
        }
        return max;
    }

    @Override
    public boolean isInfiniteDuration() {
        for (BleedingEffectInstance child : this.children) {
            if (child.isInfiniteDuration()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean endsWithin(int duration) {
        return !this.isInfiniteDuration() && this.getDuration() <= duration;
    }

    @Override
    public int mapDuration(Int2IntFunction mapper) {
        int duration = this.getDuration();
        return !this.isInfiniteDuration() && duration != 0 ? mapper.applyAsInt(duration) : duration;
    }

    @Override
    public boolean isAmbient() {
        BleedingEffectInstance representative = this.representative();
        return representative != null && representative.isAmbient();
    }

    @Override
    public boolean isVisible() {
        BleedingEffectInstance representative = this.representative();
        return representative == null || representative.isVisible();
    }

    @Override
    public boolean showIcon() {
        BleedingEffectInstance representative = this.representative();
        return representative == null || representative.showIcon();
    }

    @Override
    public boolean update(MobEffectInstance other) {
        return this.pushChild(other);
    }

    @Override
    public boolean tickServer(ServerLevel level, LivingEntity entity, Runnable onExpirationRunnable) {
        int levelBefore = this.totalLevel();
        this.expireChildren();
        if (this.children.isEmpty()) {
            return false;
        }

        int amplifier = this.getAmplifier();
        MobEffect effect = this.getEffect().value();

        // 合并结算：整个容器每个间隔只结算一次，等级为所有子实例之和。
        // 结算上下文压入容器自身，效果逻辑据此读取未截断的 totalLevel()；
        // 来源归因由 EffectSources#attributionSourceOf 按子实例来源决定（玩家优先，否则最近一次）。
        EffectSources.beginTick(this);
        try {
            if (effect.shouldApplyEffectTickThisTick(this.pulseDuration(entity), amplifier)
                && !effect.applyEffectTick(level, entity, amplifier)) {
                // 原版语义：效果判定自行结束 → 整个效果槽位移除
                this.children.clear();
                this.levelDirty = true;
                return false;
            }
        } finally {
            EffectSources.endTick();
        }

        for (BleedingEffectInstance child : this.children) {
            if (!child.isInfiniteDuration()) {
                ((MobEffectInstanceStackAccess) (Object) child).newnew$setDuration(child.getDuration() - 1);
            }
        }

        // 等级只因到期而下降时補发一次更新，让属性修饰符等依赖等级的逻辑跟着刷新
        if (levelBefore != this.totalLevel()) {
            ((LivingEntityEffectHooks) (Object) entity).newnew$onEffectUpdated(this, true, null);
        }

        return !this.children.isEmpty();
    }

    @Override
    public void onMobHurt(ServerLevel level, LivingEntity entity, DamageSource source, float amount) {
        // 与原版一致：本效果每个间隔只结算一次，等级用聚合等级
        this.getEffect().value().onMobHurt(level, entity, this.getAmplifier(), source, amount);
    }

    @Override
    public void onMobRemoved(ServerLevel level, LivingEntity entity, Entity.RemovalReason reason) {
        for (BleedingEffectInstance child : this.children) {
            child.onMobRemoved(level, entity, reason);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MobEffectInstance instance)) {
            return false;
        }
        return this.getDuration() == instance.getDuration()
            && this.getAmplifier() == instance.getAmplifier()
            && this.isAmbient() == instance.isAmbient()
            && this.isVisible() == instance.isVisible()
            && this.showIcon() == instance.showIcon()
            && this.getEffect().equals(instance.getEffect());
    }

    @Override
    public int hashCode() {
        int hash = this.getEffect().hashCode();
        hash = 31 * hash + this.getDuration();
        hash = 31 * hash + this.getAmplifier();
        hash = 31 * hash + (this.isAmbient() ? 1 : 0);
        hash = 31 * hash + (this.isVisible() ? 1 : 0);
        return 31 * hash + (this.showIcon() ? 1 : 0);
    }

    /** 剔除已到期的子实例；有变化时让等级缓存失效 */
    private void expireChildren() {
        boolean removed = false;
        for (int i = this.children.size() - 1; i >= 0; i--) {
            BleedingEffectInstance child = this.children.get(i);
            if (!child.isInfiniteDuration() && child.getDuration() <= 0) {
                this.children.remove(i);
                removed = true;
            }
        }
        if (removed) {
            this.levelDirty = true;
        }
    }

    /**
     * {@return 本次结算使用的相位时长}
     * <p>沿用原版 {@code 剩余时长 % 间隔 == 0} 的判定：取子实例剩余时长的最大值；
     * 含无限子实例时退化为 {@code entity.tickCount}，与原版无限效果一致。</p>
     */
    private int pulseDuration(LivingEntity entity) {
        int max = 0;
        for (BleedingEffectInstance child : this.children) {
            if (child.isInfiniteDuration()) {
                return entity.tickCount;
            }
            max = Math.max(max, child.getDuration());
        }
        return max;
    }

    /** {@return 首个存活子实例；列表为空时为 null} */
    @Nullable
    private BleedingEffectInstance representative() {
        return this.children.isEmpty() ? null : this.children.get(0);
    }

    /** 统一子实例类型：普通实例按原版复制构造升级，保留时长、等级与来源 */
    private static BleedingEffectInstance asTyped(MobEffectInstance instance) {
        return instance instanceof BleedingEffectInstance typed ? typed : new BleedingEffectInstance(instance);
    }
}
