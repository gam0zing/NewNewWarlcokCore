package com.gam0zing.newnew_warlock_core.loot;

import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 掉落事件入口。
 */
public final class LootEvt {

    public static void onLivingDrops(LivingDropsEvent event) {
        LootScaling.scaleDrops(event);
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        LootScaling.doubleOreDrops(event);
    }

    private LootEvt() {}
}
