package com.gam0zing.newnew_warlock_core.mixin.compat.bettercombat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.gam0zing.newnew_warlock_core.compat.bettercombat.BetterCombatGate;

/**
 * 取消【突进】附魔在 BC 矛上的原有效果。
 * <p>【突进】的唯一作用点是 {@code post_piercing_attack}；BC 存在且手持矛时直接跳过整条结算，
 * 其位移、饥饿、耐久与音效都不再触发。未安装 BC 时该配置整体不应用，附魔保持原版。</p>
 */
@Mixin(EnchantmentHelper.class)
public abstract class LungeEnchantmentMixin {

    @Inject(method = "doPostPiercingAttackEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private static void newnew$disableLungeOnSpear(ServerLevel level, LivingEntity user, CallbackInfo ci) {
        if (BetterCombatGate.isSpear(user.getWeaponItem())) {
            ci.cancel();
        }
    }
}
