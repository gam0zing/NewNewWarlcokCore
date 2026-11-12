package com.gam0zing.newnew_warlock_core.mob.net;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.gam0zing.newnew_warlock_core.mob.DifficultyDisplay;
import com.gam0zing.newnew_warlock_core.mob.MobDifficulty;
import com.gam0zing.newnew_warlock_core.mob.config.MobDifficultyConfig;

/**
 * 当前位置总难度的同步。
 * <p>每 tick 在服务端为每个玩家重算一次 {@link MobDifficulty#rawDifficulty}（包含游戏天数、
 * 垂直/水平距离、传送门进度、维度与群系全部因素），同时带上服务器游戏难度偏移
 * {@link MobDifficultyConfig#difficultyOffset} 与通用难度档位；只有数值变化时才下发，
 * 玩家静止或难度未变时不产生任何数据包。</p>
 * <p>客户端收到后写入 {@link DifficultyDisplay}，HUD 只读该值，因此显示值与服务端判定完全一致，
 * 联机时也不会因客户端读不到服务端配置或存档进度而偏低。偏移只用于显示，不参与难度本身的求和。</p>
 */
public final class DifficultySync {

    /** 数值变化小于该阈值时视为未变化，避免浮点抖动造成每 tick 发包 */
    private static final double CHANGE_EPSILON = 1.0E-4D;

    /** 上一次下发的难度与显示参数，用于变化检测 */
    private record Sent(
        double difficulty, double offset, double mediumThreshold, double highThreshold) {}

    /** 每个玩家上一次下发的数值，用于变化检测 */
    private static final Map<UUID, Sent> LAST_SENT = new HashMap<>();

    private DifficultySync() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(DifficultySync::registerPayloads);
        NeoForge.EVENT_BUS.addListener(DifficultySync::onServerTick);
        NeoForge.EVENT_BUS.addListener(DifficultySync::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(DifficultySync::onServerStopped);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
            DifficultySyncPayload.TYPE, DifficultySyncPayload.STREAM_CODEC, DifficultySync::handle);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            double difficulty = MobDifficulty.rawDifficulty(player);
            double offset = MobDifficultyConfig.difficultyOffset(player.level().getDifficulty());
            double medium = MobDifficultyConfig.mediumThreshold();
            double high = MobDifficultyConfig.highThreshold();
            Sent previous = LAST_SENT.get(player.getUUID());
            if (previous != null
                    && Math.abs(previous.difficulty() - difficulty) < CHANGE_EPSILON
                    && Math.abs(previous.offset() - offset) < CHANGE_EPSILON
                    && Math.abs(previous.mediumThreshold() - medium) < CHANGE_EPSILON
                    && Math.abs(previous.highThreshold() - high) < CHANGE_EPSILON) {
                continue;
            }
            LAST_SENT.put(player.getUUID(), new Sent(difficulty, offset, medium, high));
            PacketDistributor.sendToPlayer(
                player, new DifficultySyncPayload(difficulty, offset, medium, high));
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        LAST_SENT.clear();
    }

    private static void handle(DifficultySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DifficultyDisplay.set(
            payload.difficulty(), payload.offset(),
            payload.mediumThreshold(), payload.highThreshold()));
    }
}
