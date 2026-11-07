package com.gam0zing.newnew_warlock_core.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import com.gam0zing.newnew_warlock_core.enchant.EnchantContext;
import com.gam0zing.newnew_warlock_core.enchant.EnchantScene;
import com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting;

/**
 * 战利品「随机附魔」入口（{@code EnchantRandomlyFunction}）。
 * <p>原版在这里均匀随机挑一条附魔，再随机 1~最大等级；改造后整段交给
 * {@link WarlockEnchanting#hooks()}，默认实现保持原版随机顺序与结果。</p>
 * <p>26.3 的村民交易也用这个函数生成附魔书（{@code given_item_modifier}）。交易场景下必须补写
 * {@code ADDITIONAL_TRADE_COST}：{@code VillagerTrade#getOffer} 用它合成最终价格，缺失时价格不足 1
 * 会导致整笔交易不生成。价格由 {@link com.gam0zing.newnew_warlock_core.enchant.WarlockEnchantHooks#tradeBookCost}
 * 决定，可自定义。</p>
 */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {

    @Shadow
    @Final
    private Optional<HolderSet<Enchantment>> options;

    @Shadow
    @Final
    private boolean onlyCompatible;

    @Shadow
    @Final
    private boolean includeAdditionalCostComponent;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void warlock$run(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        RandomSource random = context.getRandom();
        boolean isBook = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
        // 只有村民交易合并交易时才带 ADDITIONAL_COST_COMPONENT_ALLOWED，据此区分交易与普通战利品。
        boolean trade = this.includeAdditionalCostComponent
            && context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED);
        EnchantScene scene = trade
            ? (isBook ? EnchantScene.TRADE_BOOK : EnchantScene.TRADE_ITEM)
            : (isBook ? EnchantScene.LOOT_BOOK : EnchantScene.LOOT_TOOL);
        Entity trader = trade ? context.getOptional(LootContextParams.THIS_ENTITY) : null;
        EnchantContext ctx = trade && trader != null
            ? EnchantContext.tradeLoot(scene, trader, context, stack, random)
            : EnchantContext.loot(scene, context, stack, random);

        ItemStack result = WarlockEnchanting.hooks().randomLootEnchant(
            ctx, stack, random, this.options, this.onlyCompatible);
        // hook 返回 null 时保持原物品，避免战利品函数把 null 传给后续流程。
        if (result == null || result.isEmpty()) {
            cir.setReturnValue(stack);
            return;
        }

        if (trade) {
            int level = isBook
                ? enchantLevel(result, DataComponents.STORED_ENCHANTMENTS)
                : enchantLevel(result, DataComponents.ENCHANTMENTS);
            int cost = isBook
                ? WarlockEnchanting.hooks().tradeBookCost(ctx, result, level, random)
                : WarlockEnchanting.hooks().tradeItemCost(ctx, result, level, random);
            result.set(DataComponents.ADDITIONAL_TRADE_COST, Math.max(1, cost));
        }
        cir.setReturnValue(result);
    }

    /** 取物品上指定附魔组件的最高等级；没有附魔时返回 0。 */
    private static int enchantLevel(ItemStack stack, DataComponentType<ItemEnchantments> component) {
        ItemEnchantments enchantments = stack.get(component);
        if (enchantments == null) {
            return 0;
        }
        int level = 0;
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            level = Math.max(level, enchantments.getLevel(holder));
        }
        return level;
    }
}
