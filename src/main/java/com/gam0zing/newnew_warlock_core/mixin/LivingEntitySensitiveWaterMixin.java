package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.combat.damage.PeriodicDamageGate;

/**
 * 怕水生物（末影人、烈焰人、炽足兽、雪傀儡）在水中/雨中的溺水。
 * <p>原版在 {@code LivingEntity#aiStep} 里每刻调用一次 {@code hurtServer(drown, 1)}，
 * 间隔由无敌帧提供；内部伤害不经过无敌帧，因此改由 {@link PeriodicDamageGate} 限速。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySensitiveWaterMixin {

    @WrapOperation(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;"
                + "Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean newnew$throttleSensitiveWater(
        LivingEntity entity, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        if (!PeriodicDamageGate.allow(entity, source)) {
            return false;
        }
        return original.call(entity, level, source, amount);
    }
}
