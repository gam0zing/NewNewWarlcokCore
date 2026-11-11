package com.gam0zing.newnew_warlock_core.enchant.net;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 只在物理客户端加载的同步辅助类。
 * <p>断开连接时丢弃服务端快照，回到 common 兜底，避免把上一个服务器的数值带进新世界。</p>
 * <p>NeoForge 2.0.148 的 {@code @EventBusSubscriber} 尚不支持在注解里指定 GAME bus，
 * 因此用静态初始化块手动注册到 {@link NeoForge#EVENT_BUS}；{@link EventBusSubscriber} 只用来保证
 * 类仅在物理客户端加载（服务端无此 sourceSet）。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class EnchantBaselineClientSync {

    static {
        NeoForge.EVENT_BUS.addListener(EnchantBaselineClientSync::onLoggingOut);
    }

    private EnchantBaselineClientSync() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EnchantBaselineConfig.clearClientSnapshot();
    }
}
