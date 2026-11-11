package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.PoisonMobEffect;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.combat.spine.WarlockHealingHost;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 邪恶化：中毒期间享受同等级的生命恢复。
 * <p>中毒的周期伤害被替换为一次 {@code healByLiving}（每跳 1 点，与生命恢复每跳治疗量相同，
 * 等级体现在中毒自身的触发频率上）。手动走 {@code healByLiving} 而不是 {@code heal}，
 * 避免与正在进行的生命恢复结算撞车；治疗量仍会经过治疗强度、受到治疗倍率与邪恶化减半的修正。</p>
 */
@Mixin(PoisonMobEffect.class)
public abstract class PoisonMobEffectMixin {

    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    private void newnew$corruptionPoisonHeals(
        ServerLevel level, LivingEntity mob, int amplification, CallbackInfoReturnable<Boolean> cir) {
        if (ModAttributes.getValue(mob, ModAttributes.CORRUPTION) > 0.0D) {
            ((WarlockHealingHost) mob).healByLiving(mob, 1.0F);
            cir.setReturnValue(true);
        }
    }
}
