package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;

import com.gam0zing.newnew_warlock_core.loot.LootScaling;

/**
 * 时运 / 抢夺等级追加。
 * <p>战利品表的奖励数量公式（{@code ApplyBonusCount}）读取附魔等级的唯一入口是
 * {@code EnchantmentHelper#getItemEnchantmentLevel}，这里把该等级换成「原始等级 + 归属生物的属性」，
 * 因此方块掉落、生物掉落与其它引用时运/抢夺的战利品表都同时生效。</p>
 */
@Mixin(ApplyBonusCount.class)
public abstract class ApplyBonusCountMixin {

    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;"
                + "getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I"))
    private int newnew$adjustBonusLevel(
        Holder<Enchantment> enchantment, ItemInstance tool, ItemStack stack, LootContext ctx) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, tool);
        return LootScaling.adjustEnchantmentLevel(enchantment, level, ctx);
    }
}
