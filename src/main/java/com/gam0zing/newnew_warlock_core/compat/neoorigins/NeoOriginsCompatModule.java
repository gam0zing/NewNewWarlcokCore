package com.gam0zing.newnew_warlock_core.compat.neoorigins;

import com.gam0zing.newnew_warlock_core.core.ModModule;
import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.WarlockOrigins;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoOrigins 可选兼容模块。
 * <p>未安装 NeoOrigins 时整个模块不注册任何监听，也不会加载兼容类。</p>
 */
public final class NeoOriginsCompatModule implements ModModule {

    @Override
    public String id() {
        return "neoorigins_compat";
    }

    @Override
    public int priority() {
        // 早于 SkillModule(60) 注册登录监听，保证加护在技能树对账前就位
        return 55;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        if (!NeoOriginsGate.isLoaded()) {
            return;
        }

        WarlockOrigins.install(new NeoOriginsOriginAccess());
        OriginPowers.POWER_TYPES.register(modEventBus);
        OriginPowers.register();
        NeoForge.EVENT_BUS.register(OriginTraits.class);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, OriginSync::onLogin);
        NeoForge.EVENT_BUS.addListener(OriginSync::onLogout);
        NeoForge.EVENT_BUS.addListener(OriginSync::onServerTick);
        NeoForge.EVENT_BUS.addListener(OriginSync::onServerStarted);
        NeoForge.EVENT_BUS.addListener(OriginSync::onServerStopped);
    }
}
