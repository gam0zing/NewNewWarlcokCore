package com.gam0zing.newnew_warlock_core.enchant;

import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 附魔基准表的生命周期入口。
 * <ul>
 *     <li>{@link AddServerReloadListenersEvent}：服务端资源加载 / {@code /reload} 开始，只重建索引（标签此时还没绑定）；</li>
 *     <li>{@link TagsUpdatedEvent}：标签绑定完成，补齐缺失条目、按标签校正开关并重建索引（唯一生成时机）；</li>
 *     <li>{@link ServerStartedEvent}：定位存档目录，生成/加载 server 表并刷新；</li>
 *     <li>{@link ServerStoppedEvent}：关闭 server 表、清空索引，回到 common；</li>
 *     <li>{@link LevelEvent.Load}：客户端进入世界时用客户端注册表重建索引（多人游戏使用 common 表）。</li>
 * </ul>
 * <p>生成必须等到 {@link TagsUpdatedEvent}：{@code AddServerReloadListenersEvent} 阶段 {@code holder.is(tag)} 还是空的，
 * 在那个时机写文件会把 {@code in_table}/{@code tradeable} 等开关全部写成 {@code false}，附魔台将没有任何候选。</p>
 */
public final class EnchantBaselineLoader {

    private EnchantBaselineLoader() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EnchantBaselineLoader::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(EnchantBaselineLoader::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(EnchantBaselineLoader::onServerStarted);
        NeoForge.EVENT_BUS.addListener(EnchantBaselineLoader::onServerStopped);
        NeoForge.EVENT_BUS.addListener(EnchantBaselineLoader::onLevelLoad);
    }

    private static void onAddReloadListener(AddServerReloadListenersEvent event) {
        if (event.getServerResources().getRegistryLookup() instanceof RegistryAccess registries) {
            EnchantBaselineConfig.refresh(registries, false);
        }
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        EnchantBaselineConfig.refresh(event.getRegistries(), true);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        EnchantBaselineConfig.loadServer(event.getServer());
        EnchantBaselineConfig.refresh(event.getServer().registryAccess(), true);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        EnchantBaselineConfig.unloadServer();
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()) {
            EnchantBaselineConfig.refresh(level.registryAccess(), true);
        }
    }
}
