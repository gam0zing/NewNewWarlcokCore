package com.gam0zing.newnew_warlock_core.effect;

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
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.EffectStack;

/**
 * 致命伤容器：把多次施加的致命伤按子实例累加成一个等级。
 *
 * <p>聚合口径与 {@link BleedingEffectStack} 一致：</p>
 * <ul>
 *     <li>{@link #getAmplifier()}：所有子实例等级之和，只在赋值给 amplifier 时截断到原版上限；</li>
 *     <li>{@link #getDuration()} / {@link #isInfiniteDuration()}：子实例时长最大值，任一无限即无限；</li>
 *     <li>{@link #isAmbient()} / {@link #isVisible()} / {@link #showIcon()}：取首个存活子实例的展示标志；</li>
 *     <li>效果逻辑需要真实等级时读 {@link #totalLevel()}，不受 255 上限截断。</li>
 * </ul>
 *
 * <p>致命伤本身没有周期结算，容器只需推进各子实例时长；到期只剔除该子实例，列表为空时整体移除。</p>
 */
public class FatalWoundEffectStack extends MobEffectInstance implements EffectStack {

    private final List<MobEffectInstance> children = new ArrayList<>();

    /** 子实例等级之和的缓存，仅在子实例增删时失效；不参与存档 */
    private int cachedLevel;
    private boolean levelDirty = true;

    public FatalWoundEffectStack(Holder<MobEffect> effect) {
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

        this.children.add(child);
        this.levelDirty = true;
        return true;
    }

    @Override
    public List<MobEffectInstance> children() {
        return Collections.unmodifiableList(new ArrayList<>(this.children));
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
        for (MobEffectInstance child : this.children) {
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
        for (MobEffectInstance child : this.children) {
            if (child.isInfiniteDuration()) {
                return MobEffectInstance.INFINITE_DURATION;
            }
            max = Math.max(max, child.getDuration());
        }
        return max;
    }

    @Override
    public boolean isInfiniteDuration() {
        for (MobEffectInstance child : this.children) {
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
        MobEffectInstance representative = this.representative();
        return representative != null && representative.isAmbient();
    }

    @Override
    public boolean isVisible() {
        MobEffectInstance representative = this.representative();
        return representative == null || representative.isVisible();
    }

    @Override
    public boolean showIcon() {
        MobEffectInstance representative = this.representative();
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

        // 与原版容器口径一致：整个容器每个间隔只结算一次，等级为所有子实例之和。
        EffectSources.beginTick(this);
        try {
            if (effect.shouldApplyEffectTickThisTick(this.pulseDuration(entity), amplifier)
                && !effect.applyEffectTick(level, entity, amplifier)) {
                this.children.clear();
                this.levelDirty = true;
                return false;
            }
        } finally {
            EffectSources.endTick();
        }

        for (MobEffectInstance child : this.children) {
            if (!child.isInfiniteDuration()) {
                ((MobEffectInstanceStackAccess) (Object) child).newnew$setDuration(child.getDuration() - 1);
            }
        }

        // 等级只因到期而下降时补发一次更新，让属性修饰符等依赖等级的逻辑跟着刷新
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
        for (MobEffectInstance child : this.children) {
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
            MobEffectInstance child = this.children.get(i);
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
        for (MobEffectInstance child : this.children) {
            if (child.isInfiniteDuration()) {
                return entity.tickCount;
            }
            max = Math.max(max, child.getDuration());
        }
        return max;
    }

    /** {@return 首个存活子实例；列表为空时为 null} */
    @Nullable
    private MobEffectInstance representative() {
        return this.children.isEmpty() ? null : this.children.get(0);
    }
}
