package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.bee.Bee;

import com.gam0zing.newnew_warlock_core.combat.damage.PeriodicDamageGate;

/**
 * 蜜蜂水下溺水。
 * <p>原版在 {@code Bee#customServerAiStep} 里，{@code underWaterTicks > 20} 之后每刻调用
 * {@code hurtServer(drown, 1)}，间隔由无敌帧提供；内部伤害不经过无敌帧，因此改由
 * {@link PeriodicDamageGate} 限速。</p>
 */
@Mixin(Bee.class)
public abstract class BeeUnderwaterDamageMixin {

    @WrapOperation(
        method = "customServerAiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/bee/Bee;hurtServer(Lnet/minecraft/server/level/ServerLevel;"
                + "Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean newnew$throttleUnderwater(
        Bee bee, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        if (!PeriodicDamageGate.allow(bee, source)) {
            return false;
        }
        return original.call(bee, level, source, amount);
    }
}
