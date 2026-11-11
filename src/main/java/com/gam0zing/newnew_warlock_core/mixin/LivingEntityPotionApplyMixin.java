package com.gam0zing.newnew_warlock_core.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.effect.Corruption;
import com.gam0zing.newnew_warlock_core.effect.PotionScaling;

/**
 * 药水属性在施加入口的修正。
 * <p>原版所有效果施加最终都会走 {@code LivingEntity#addEffect(MobEffectInstance, Entity)}，
 * 因此在这里按来源生物的药水等级 / 药水时间属性改写实例；等级被降到 0 以下时放弃这次施加。
 * 邪恶化目标对凋零的免疫也在同一入口拦截。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPotionApplyMixin {

    @Inject(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void newnew$scaleIncomingEffect(
        MobEffectInstance instance, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (!Corruption.allowsEffect(target, instance)) {
            cir.setReturnValue(false);
            return;
        }
        if (!PotionScaling.apply(target, instance, source)) {
            cir.setReturnValue(false);
        }
    }
}
