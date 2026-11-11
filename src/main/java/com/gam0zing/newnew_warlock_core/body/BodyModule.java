package com.gam0zing.newnew_warlock_core.body;

import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 身体模块：受击部位数据文件（{@code data/<命名空间>/newnew_body/*.json}）的加载器。
 */
public final class BodyModule implements ModModule {

    @Override
    public String id() {
        return "body";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(BodyPartLoader::onAddReloadListener);
    }
}
