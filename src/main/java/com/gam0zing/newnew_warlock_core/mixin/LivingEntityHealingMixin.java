package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.combat.spine.Healing;
import com.gam0zing.newnew_warlock_core.combat.spine.WarlockHealingHost;

/**
 * 给 {@link LivingEntity} 增加 {@code healByLiving(LivingEntity source, float amount)}。
 * <p>计算交给 {@link Healing}（业务区域），落地时调用生物自身的 {@link LivingEntity#heal(float)}，
 * 因此 {@code LivingHealEvent} 等外部钩子照常触发。</p>
 * <p>此外把“效果结算内产生的治疗”（再生、瞬时治疗等）按来源路由到 {@code healByLiving}：
 * 这样治疗强度等来源相关规则才会生效。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealingMixin implements WarlockHealingHost {

    /** 防止 {@link #healByLiving} 回落调用 {@code heal} 时再次进入来源路由 */
    @Unique
    private static final ThreadLocal<Boolean> NEWNEW$HEALING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** 效果结算内的 {@code heal} 改为带来源治疗；自己喝药/进食的瞬时治疗同样归因使用者 */
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void newnew$healFromEffectSource(float amount, CallbackInfo ci) {
        if (NEWNEW$HEALING.get()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        LivingEntity source = EffectSources.currentTickingSource(self);
        if (source == null) {
            source = EffectSources.currentSelfConsumptionSource(self);
        }
        if (source == null) {
            return;
        }

        NEWNEW$HEALING.set(Boolean.TRUE);
        try {
            this.healByLiving(source, amount);
        } finally {
            NEWNEW$HEALING.set(Boolean.FALSE);
        }
        ci.cancel();
    }

    @Override
    public float healByLiving(LivingEntity source, float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        float healed = Healing.calculate(self, source, amount);
        if (healed <= 0.0F) {
            return 0.0F;
        }

        float before = self.getHealth();
        self.heal(healed);
        return Math.max(0.0F, self.getHealth() - before);
    }
}
