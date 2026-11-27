package com.gam0zing.newnew_warlock_core.origin.content;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.origin.OriginIds;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 起源内容的物品注册。
 */
public final class OriginItems {

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(NewNewWarlockCore.MOD_ID);

    public static final DeferredItem<OriginOrbItem> ORIGIN_ORB =
        ITEMS.registerItem("origin_orb", OriginOrbItem::new, () -> new Item.Properties().stacksTo(1));

    public static final DeferredItem<ForgettingBottleItem> FORGETTING_BOTTLE =
        ITEMS.registerItem(
            "forgetting_bottle", ForgettingBottleItem::new, () -> new Item.Properties().stacksTo(16));

    public static final DeferredItem<EssenceBottleItem> ESSENCE_BOTTLE =
        ITEMS.registerItem(
            "essence_bottle",
            properties -> new EssenceBottleItem(properties, OriginIds.ESSENCE_EMPTY),
            () -> new Item.Properties().stacksTo(16));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> SAMPLE_STATUE =
        ITEMS.registerSimpleBlockItem(OriginBlocks.SAMPLE_STATUE);

    private OriginItems() {}
}
