package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.dolphin.Dolphin;

import com.gam0zing.newnew_warlock_core.combat.damage.PeriodicDamageGate;

/**
 * 海豚脱水。
 * <p>原版在 {@code Dolphin#tick} 里，湿度归零后每刻调用 {@code hurt(dry_out, 1)}，
 * 间隔由无敌帧提供；内部伤害不经过无敌帧，因此改由 {@link PeriodicDamageGate} 限速。</p>
 */
@Mixin(Dolphin.class)
public abstract class DolphinDryOutMixin {

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/dolphin/Dolphin;hurt(Lnet/minecraft/world/damagesource/"
                + "DamageSource;F)V"))
    private void newnew$throttleDryOut(
        Dolphin dolphin, DamageSource source, float amount, Operation<Void> original) {
        if (!PeriodicDamageGate.allow(dolphin, source)) {
            return;
        }
        original.call(dolphin, source, amount);
    }
}
