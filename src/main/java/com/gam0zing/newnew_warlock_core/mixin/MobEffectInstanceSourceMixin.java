package com.gam0zing.newnew_warlock_core.mixin;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.combat.effect.EffectSources;
import com.gam0zing.newnew_warlock_core.combat.effect.WarlockEffectHost;

/**
 * 给 {@link MobEffectInstance} 增加“来源生物”的可空引用 + 来源 UUID。
 * <p>运行时用引用做归因；引用丢失时用 UUID 懒解析，因此这组状态是服务端能力：</p>
 * <ul>
 *     <li>复制构造与同效果刷新（{@code update}）会带上来源与 UUID，药水取效果、叠加刷新不丢归属；</li>
 *     <li>{@code save}/{@code load} 把 UUID 写入/读出实体存档（只此一处使用，不涉及物品组件与网络包）；</li>
 *     <li>{@code tick} 期间把当前实例压入 {@link EffectSources} 的结算栈，
 *     使 {@code applyEffectTick} 内产生的 {@code hurt}/{@code heal} 能读到来源。</li>
 * </ul>
 * <p>来源不参与 {@code equals}/{@code hashCode}、不进入 {@code CODEC}/{@code STREAM_CODEC}。
 * 已知边界（隐藏效果只随外层保存、来源解析失败按无来源等）见 {@link EffectSources} 的“已知边界”一节。</p>
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceSourceMixin implements WarlockEffectHost {

    /** 实体存档里保存来源 UUID 的键 */
    @Unique
    private static final String NEWNEW$SOURCE_KEY = "newnew:source";

    @Unique
    @Nullable
    private LivingEntity newnew$effectSource;

    @Unique
    @Nullable
    private UUID newnew$effectSourceUuid;

    @Override
    @Nullable
    public LivingEntity newnew$effectSource() {
        return this.newnew$effectSource;
    }

    @Override
    public void newnew$setEffectSource(@Nullable LivingEntity source) {
        this.newnew$effectSource = source;
        this.newnew$effectSourceUuid = source == null ? null : source.getUUID();
    }

    @Override
    @Nullable
    public UUID newnew$effectSourceUuid() {
        return this.newnew$effectSourceUuid;
    }

    @Override
    public void newnew$setEffectSourceUuid(@Nullable UUID uuid) {
        this.newnew$effectSourceUuid = uuid;
    }

    /** 复制构造：来源引用与 UUID 随实例一起复制（{@code PotionContents} 取效果正是走这个构造器） */
    @Inject(method = "<init>(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("RETURN"))
    private void newnew$copyEffectSource(MobEffectInstance other, CallbackInfo ci) {
        this.newnew$setEffectSource(EffectSources.sourceOf(other));
        this.newnew$effectSourceUuid = EffectSources.sourceUuidOf(other);
    }

    /** 同效果刷新：新实例带来源/UUID时接替旧归属（新实例没有来源则保留旧归属） */
    @Inject(method = "update", at = @At("RETURN"))
    private void newnew$inheritEffectSource(MobEffectInstance other, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity source = EffectSources.sourceOf(other);
        if (source != null) {
            this.newnew$setEffectSource(source);
        }
        UUID uuid = EffectSources.sourceUuidOf(other);
        if (uuid != null) {
            LivingEntity current = this.newnew$effectSource;
            if (current != null && !uuid.equals(current.getUUID())) {
                this.newnew$effectSource = null;
            }
            this.newnew$effectSourceUuid = uuid;
        }
    }

    /** 结算开始：记录当前效果实例，供伤害/治疗入口读取来源 */
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void newnew$beginTick(
        ServerLevel level, LivingEntity entity, Runnable onExpirationRunnable,
        CallbackInfoReturnable<Boolean> cir) {
        EffectSources.beginTick((MobEffectInstance) (Object) this);
    }

    /** 结算结束：弹出当前效果实例（与 {@code newnew$beginTick} 成对） */
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void newnew$endTick(
        ServerLevel level, LivingEntity entity, Runnable onExpirationRunnable,
        CallbackInfoReturnable<Boolean> cir) {
        EffectSources.endTick();
    }
}
