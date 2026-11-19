package com.gam0zing.newnew_warlock_core.mixin.compat.bettercombat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gam0zing.newnew_warlock_core.compat.bettercombat.BetterCombatGate;
import com.gam0zing.newnew_warlock_core.compat.bettercombat.SpearLift;
import com.gam0zing.newnew_warlock_core.compat.bettercombat.SpearLiftContext;

/**
 * BC 矛左键的伤害源替换。
 * <p>只在 {@link SpearLiftContext} 圈定的 BC 左键结算内生效，把矛的
 * {@code minecraft:spear}（带 {@code no_knockback}）换成会进入物理管线、
 * 从而能产生挑飞位移的 {@code newnew_warlock_core:spear_lift}。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackDamageSourceMixin {

    @Inject(method = "getDamageSource", at = @At("HEAD"), cancellable = true)
    private void newnew$spearLiftSource(LivingEntity attacker, CallbackInfoReturnable<DamageSource> cir) {
        if (!SpearLiftContext.isActive()) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (!BetterCombatGate.isSpear(self)) {
            return;
        }
        DamageSource source = SpearLift.createSource(attacker);
        if (source != null) {
            cir.setReturnValue(source);
        }
    }
}
