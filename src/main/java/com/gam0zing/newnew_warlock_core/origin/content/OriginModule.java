package com.gam0zing.newnew_warlock_core.origin.content;

import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * 起源内容模块：注册物品与方块。
 * <p>即使未安装 NeoOrigins 也注册这些内容，使用时再提示不可用。</p>
 */
public final class OriginModule implements ModModule {

    @Override
    public String id() {
        return "origin";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        OriginBlocks.BLOCKS.register(modEventBus);
        OriginItems.ITEMS.register(modEventBus);
    }
}
