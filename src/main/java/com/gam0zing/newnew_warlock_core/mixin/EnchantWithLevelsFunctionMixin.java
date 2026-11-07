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
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

import com.gam0zing.newnew_warlock_core.enchant.EnchantContext;
import com.gam0zing.newnew_warlock_core.enchant.EnchantScene;
import com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting;

/**
 * 战利品「按等级附魔」入口（{@code EnchantWithLevelsFunction}），例如宝箱里的附魔装备。
 * <p>原版调用 {@code EnchantmentHelper#enchantItem}；改造后整段交给
 * {@link WarlockEnchanting#hooks()}，默认实现仍然调用原版方法。</p>
 * <p>26.3 的村民装备交易也用这个函数（{@code given_item_modifier}）。交易场景下补写
 * {@code ADDITIONAL_TRADE_COST}，价格由
 * {@link com.gam0zing.newnew_warlock_core.enchant.WarlockEnchantHooks#tradeItemCost} 决定，可自定义。</p>
 */
@Mixin(EnchantWithLevelsFunction.class)
public abstract class EnchantWithLevelsFunctionMixin {

    @Shadow
    @Final
    private Holder<ContextIntProvider> levels;

    @Shadow
    @Final
    private Optional<HolderSet<Enchantment>> options;

    @Shadow
    @Final
    private boolean includeAdditionalCostComponent;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void warlock$run(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        RandomSource random = context.getRandom();
        int level = this.levels.value().getInt(context);
        // 只有村民交易合并交易时才带 ADDITIONAL_COST_COMPONENT_ALLOWED，据此区分交易与普通战利品。
        boolean trade = this.includeAdditionalCostComponent
            && context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED);
        Entity trader = trade ? context.getOptional(LootContextParams.THIS_ENTITY) : null;
        EnchantContext ctx = trade && trader != null
            ? EnchantContext.tradeLoot(EnchantScene.TRADE_ITEM, trader, context, stack, random)
            : EnchantContext.loot(EnchantScene.LOOT_TOOL, context, stack, random);

        ItemStack result = WarlockEnchanting.hooks().leveledLootEnchant(ctx, stack, level, random, this.options);
        // hook 返回 null 时保持原物品，避免战利品函数把 null 传给后续流程。
        if (result == null || result.isEmpty()) {
            cir.setReturnValue(stack);
            return;
        }

        if (trade && level > 0) {
            int cost = WarlockEnchanting.hooks().tradeItemCost(ctx, result, level, random);
            result.set(DataComponents.ADDITIONAL_TRADE_COST, Math.max(1, cost));
        }
        cir.setReturnValue(result);
    }
}
