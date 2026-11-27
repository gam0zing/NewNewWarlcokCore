package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.gam0zing.newnew_warlock_core.mob.ExplorerBonus;

/**
 * 探险家：吸收经验球时按概率使经验数值翻倍。
 * <p>作用点是经验球把剩余经验（经验修补消耗之后）交给玩家的那一刻，
 * 因此命令、任务等其它经验来源不受影响，也不会干扰经验修补的结算。</p>
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Redirect(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void newnew$explorerDoubleXp(Player player, int amount) {
        player.giveExperiencePoints(ExplorerBonus.doubleOrbXp(player, amount));
    }

    /**
     * 经验修补按等级提升「经验 → 耐久」的转换效率：I 级与原版一致，每高一级多修 1 倍耐久。
     * <p>原版在这一步把经验值换算成可修复耐久，并按比例回退消耗的经验；放大换算结果后
     * 消耗的经验随之减少，因此只改变效率，不影响经验总量。</p>
     */
    @WrapOperation(
        method = "repairPlayerItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int newnew$scaleMending(
        ServerLevel level, ItemStack stack, int repair, Operation<Integer> original) {
        int mendingLevel = mendingLevel(stack);
        if (mendingLevel > 1) {
            repair = (int) Math.min((long) repair * mendingLevel, Integer.MAX_VALUE);
        }
        return original.call(level, stack, repair);
    }

    private static int mendingLevel(ItemStack stack) {
        ItemEnchantments enchantments = stack.getEnchantments();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(Enchantments.MENDING)) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }
}
