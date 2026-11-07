package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.gam0zing.newnew_warlock_core.combat.damage.DamagePipeline;

/**
 * 攻击方工具耐久的断流。
 * <p>原版 {@code Player#attack} 命中后调用 {@code ItemStack#hurtEnemy(target, player)} 扣武器耐久。
 * 这里把它转到业务插槽 {@link DamagePipeline#applyToolDurability}，插槽默认不放行，
 * 因此默认不会消耗工具耐久（也不会触发物品自身的 {@code postHurtEnemy} 逻辑）。</p>
 */
@Mixin(Player.class)
public abstract class PlayerAttackEquipmentMixin {

    @WrapOperation(
        method = "itemAttackInteraction(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/damagesource/DamageSource;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtEnemy(Lnet/minecraft/world/entity/LivingEntity;"
                + "Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean newnew$hurtEnemy(
        ItemStack stack, LivingEntity target, LivingEntity attacker, Operation<Boolean> original) {
        if (!DamagePipeline.applyToolDurability((Player) (Object) this, stack, target)) {
            return false;
        }
        return original.call(stack, target, attacker);
    }
}
