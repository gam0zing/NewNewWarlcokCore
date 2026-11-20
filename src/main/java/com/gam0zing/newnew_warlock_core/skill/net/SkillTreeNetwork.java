package com.gam0zing.newnew_warlock_core.skill.net;

import com.gam0zing.newnew_warlock_core.origin.event.PlayerOriginChangedEvent;
import com.gam0zing.newnew_warlock_core.skill.SkillTreeService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 技能树的网络入口：定义/进度下发、解锁/重置请求。
 */
public final class SkillTreeNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private SkillTreeNetwork() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SkillTreeNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(SkillTreeNetwork::onDatapackSync);
        // 放在 LOWEST：确保 NeoOrigins 的登录/首次选择逻辑先执行
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillTreeNetwork::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(SkillTreeNetwork::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(SkillTreeNetwork::onOriginChanged);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
            .playToClient(SkillTreeSyncPayload.TYPE, SkillTreeSyncPayload.STREAM_CODEC, SkillTreeNetwork::handleTrees)
            .playToClient(SkillProgressSyncPayload.TYPE, SkillProgressSyncPayload.STREAM_CODEC,
                SkillTreeNetwork::handleProgress)
            .playToServer(UnlockNodePayload.TYPE, UnlockNodePayload.STREAM_CODEC, SkillTreeNetwork::handleUnlock)
            .playToServer(ResetTreePayload.TYPE, ResetTreePayload.STREAM_CODEC, SkillTreeNetwork::handleReset);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        for (ServerPlayer player : event.getRelevantPlayers().toList()) {
            SkillTreeService.reconcile(player);
            SkillTreeService.syncAll(player);
        }
    }

    private static void onOriginChanged(PlayerOriginChangedEvent event) {
        SkillTreeService.reconcile(event.player());
        SkillTreeService.syncAll(event.player());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillTreeService.onLogin(player);
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 死亡重生会换成新的玩家实体，永久属性修饰符不会随 clone 复制
            SkillTreeService.onRespawn(player, event.isEndConquered());
        }
    }

    private static void handleTrees(SkillTreeSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> com.gam0zing.newnew_warlock_core.skill.client.SkillTreeClientData.acceptTrees(payload));
    }

    private static void handleProgress(SkillProgressSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(
            () -> com.gam0zing.newnew_warlock_core.skill.client.SkillTreeClientData.acceptProgress(payload));
    }

    private static void handleUnlock(UnlockNodePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> SkillTreeService.unlockNode(player, payload.treeId(), payload.nodeId()));
        }
    }

    private static void handleReset(ResetTreePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> SkillTreeService.resetTree(player, payload.treeId()));
        }
    }
}
