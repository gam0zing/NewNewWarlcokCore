package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gam0zing.newnew_warlock_core.share.ModMobEffects;
import com.gam0zing.newnew_warlock_core.share.SlimeDehydratedMobEffect;
import com.gam0zing.newnew_warlock_core.share.SlimeFrozenMobEffect;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 冰冻 / 失水降低跳跃初速，跳跃高度按平方下降。
 * <p>客户端与服务端都会走 {@code getJumpPower}，而效果本身是同步的，
 * 因此两端算出的初速一致，不会出现服务端回拉。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimateJumpMixin {

    @ModifyReturnValue(method = "getJumpPower(F)F", at = @At("RETURN"))
    private float newnew$climateJumpPower(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        float reduction = 0.0F;
        MobEffectInstance frozen = self.getEffect(ModMobEffects.SLIME_FROZEN);
        if (frozen != null) {
            reduction += SlimeFrozenMobEffect.reductionFor(frozen.getAmplifier() + 1);
        }
        MobEffectInstance dehydrated = self.getEffect(ModMobEffects.SLIME_DEHYDRATED);
        if (dehydrated != null) {
            reduction += SlimeDehydratedMobEffect.reductionFor(dehydrated.getAmplifier() + 1);
        }
        return original * (1.0F - Mth.clamp(reduction, 0.0F, 1.0F));
    }
}
