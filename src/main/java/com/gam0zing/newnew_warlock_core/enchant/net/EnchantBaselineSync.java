package com.gam0zing.newnew_warlock_core.enchant.net;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

/**
 * 基准快照同步（方案 B）。
 * <p>服务端在玩家加入、{@code /reload} 完成后下发快照；客户端收到后替换本地索引，
 * 从而让工具提示/创造物品栏显示的等级上限与世界内判定一致。</p>
 * <p>基准表为空（配置未初始化、重载失败）时<b>不下发</b>，客户端继续用本地 common 表；
 * 否则客户端会把快照标志锁在空表上，直到重连才能恢复。</p>
 * <p>客户端只读取快照，不参与玩家相关计算；玩家相关规则仍由服务端入口传入
 * {@link com.gam0zing.newnew_warlock_core.enchant.EnchantContext} 计算。</p>
 */
public final class EnchantBaselineSync {

    private EnchantBaselineSync() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EnchantBaselineSync::registerPayloads);
        NeoForge.EVENT_BUS.addListener(EnchantBaselineSync::onDatapackSync);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            // 只在物理客户端加载引用 client-only 事件的类，避免专用服务端类加载失败。
            EnchantBaselineClientSync.register();
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
            EnchantBaselineSyncPayload.TYPE, EnchantBaselineSyncPayload.STREAM_CODEC, EnchantBaselineSync::handle);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        EnchantBaselineSyncPayload payload = createPayload();
        if (payload == null) {
            // 没有可用基准表时不下发：客户端会保留自己的 common 表，而不是被空快照锁死。
            return;
        }
        for (ServerPlayer player : event.getRelevantPlayers().toList()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /**
     * 组装当前生效基准表的快照。
     *
     * @return 快照；基准表为空（未初始化/重载失败）或条数超过协议上限时返回 {@code null}，调用方应跳过下发。
     *         空快照绝不能发给客户端：客户端收到后会把 {@code clientSnapshot} 锁在空表上直到重登。
     */
    @Nullable
    public static EnchantBaselineSyncPayload createPayload() {
        List<EnchantBaseline.Entry> entries = List.copyOf(EnchantBaseline.entries().values());
        if (entries.isEmpty()) {
            NewNewWarlockCore.LOGGER.warn("[enchant] 基准表为空，跳过基准快照同步");
            return null;
        }
        if (entries.size() > EnchantBaselineSyncPayload.MAX_ENTRIES) {
            NewNewWarlockCore.LOGGER.error(
                "[enchant] 基准表条目 {} 超过协议上限 {}，跳过基准快照同步",
                entries.size(), EnchantBaselineSyncPayload.MAX_ENTRIES);
            return null;
        }
        return new EnchantBaselineSyncPayload(
            entries, EnchantBaselineConfig.settings(), EnchantBaselineSyncPayload.SCHEMA_VERSION);
    }

    private static void handle(EnchantBaselineSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }

            try {
                EnchantBaselineConfig.applySnapshot(
                    player.level().registryAccess(), payload.entries(), payload.settings());
            } catch (Exception exception) {
                NewNewWarlockCore.LOGGER.warn("[enchant] 应用服务端基准快照失败，继续使用本地表", exception);
            }
        });
    }
}
