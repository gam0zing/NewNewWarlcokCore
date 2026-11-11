package com.gam0zing.newnew_warlock_core.loot;

import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 掉落模块：时运/抢夺等级追加与击杀掉落倍率。
 */
public final class LootModule implements ModModule {

    @Override
    public String id() {
        return "loot";
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(LootEvt::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(LootEvt::onBlockDrops);
    }
}
