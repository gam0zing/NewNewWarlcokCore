package com.gam0zing.newnew_warlock_core.mixin.compat.bettercombat;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 原版矛冲锋伤害调整。
 * <p>原公式为 {@code B + ⌊相对速度 × 倍率⌋}，随后 {@code stabAttack} 再平加锋利等附魔。</p>
 * <p>现在把速度项按锋利等级放大：
 * {@code B + 0.75 × ⌊相对速度 × 倍率 × (1 + 0.15 × 锋利等级)⌋}，附魔平加值照旧由 {@code stabAttack} 补上。
 * 相对速度不设上限。</p>
 */
@Mixin(KineticWeapon.class)
public abstract class KineticWeaponMixin {

    @ModifyArg(
        method = "damageEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;floor(D)I"),
        require = 0)
    private double newnew$scaleSpeedDamageBySharpness(double speedDamage,
        @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) LivingEntity user) {
        Holder<Enchantment> sharpness = user.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SHARPNESS);
        int level = EnchantmentHelper.getItemEnchantmentLevel(sharpness, stack);
        return 0.75D * speedDamage * (1.0D + 0.15D * level);
    }
}
