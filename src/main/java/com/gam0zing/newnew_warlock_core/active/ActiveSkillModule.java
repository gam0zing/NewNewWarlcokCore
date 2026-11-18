package com.gam0zing.newnew_warlock_core.active;

import com.gam0zing.newnew_warlock_core.active.net.FlashStepPayload;
import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 主动技能模块：客户端采集输入、服务端判定与生效。
 */
public final class ActiveSkillModule implements ModModule {

    @Override
    public String id() {
        return "active";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ActiveSkillModule::registerPayloads);
        NeoForge.EVENT_BUS.addListener(FlashStepService::onPlayerLoggedOut);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
            .playToServer(FlashStepPayload.TYPE, FlashStepPayload.STREAM_CODEC, FlashStepService::handle);
    }
}
