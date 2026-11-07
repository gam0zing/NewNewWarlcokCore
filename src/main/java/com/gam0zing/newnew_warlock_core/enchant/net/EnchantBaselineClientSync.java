package com.gam0zing.newnew_warlock_core.enchant.net;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

/**
 * 只在物理客户端加载的同步辅助类。
 * <p>断开连接时丢弃服务端快照，回到 common 兜底，避免把上一个服务器的数值带进新世界。</p>
 */
public final class EnchantBaselineClientSync {

    private EnchantBaselineClientSync() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EnchantBaselineClientSync::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EnchantBaselineConfig.clearClientSnapshot();
    }
}
