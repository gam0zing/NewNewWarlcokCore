package com.gam0zing.newnew_warlock_core.compat.neoorigins;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.cyberday1.neoorigins.api.origin.OriginLayer;
import com.cyberday1.neoorigins.config.GameplayConfig;
import com.cyberday1.neoorigins.data.LayerDataManager;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.origin.OriginIds;
import com.gam0zing.newnew_warlock_core.origin.OriginSetResult;
import com.gam0zing.newnew_warlock_core.origin.OriginSlot;
import com.gam0zing.newnew_warlock_core.origin.WarlockOrigins;
import com.gam0zing.newnew_warlock_core.origin.event.PlayerOriginChangedEvent;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoOrigins 数据与玩家状态的对账器。
 * <p>NeoOrigins 的 {@code OriginChangedEvent} 覆盖不到 {@code clearOrigin} 与级联清除，
 * 因此这里按固定间隔比较快照：发现差异时逐槽位发布 {@link PlayerOriginChangedEvent}，
 * 技能树等监听方据此刷新自己的状态。</p>
 */
public final class OriginSync {

    /** 快照比较间隔（tick） */
    private static final int INTERVAL_TICKS = 5;

    /** 只记录有起源的槽位 */
    private static final Map<UUID, EnumMap<OriginSlot, Identifier>> SNAPSHOTS = new HashMap<>();

    private static int tickCounter;

    private OriginSync() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureBlessing(player);
        SNAPSHOTS.put(player.getUUID(), snapshot(player));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SNAPSHOTS.remove(player.getUUID());
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            reconcile(player);
        }
    }

    private static void reconcile(ServerPlayer player) {
        // 首次选择可能清空加护，这里在每次对账前补回空白加护
        ensureBlessing(player);

        EnumMap<OriginSlot, Identifier> current = snapshot(player);
        EnumMap<OriginSlot, Identifier> previous = SNAPSHOTS.get(player.getUUID());
        if (previous == null) {
            SNAPSHOTS.put(player.getUUID(), current);
            return;
        }

        boolean changed = false;
        for (OriginSlot slot : OriginSlot.values()) {
            Identifier oldOrigin = previous.get(slot);
            Identifier newOrigin = current.get(slot);
            if (Objects.equals(oldOrigin, newOrigin)) {
                continue;
            }
            NeoForge.EVENT_BUS.post(new PlayerOriginChangedEvent(player, slot, oldOrigin, newOrigin));
            changed = true;
        }
        if (changed) {
            SNAPSHOTS.put(player.getUUID(), current);
        }
    }

    private static EnumMap<OriginSlot, Identifier> snapshot(ServerPlayer player) {
        EnumMap<OriginSlot, Identifier> snapshot = new EnumMap<>(OriginSlot.class);
        snapshot.putAll(WarlockOrigins.snapshot(player));
        return snapshot;
    }

    /** 加护为空时补上空白加护；层缺失只记录一次调试日志。 */
    private static void ensureBlessing(ServerPlayer player) {
        if (WarlockOrigins.getOrigin(player, OriginSlot.BLESSING).isPresent()) {
            return;
        }
        OriginSetResult result = WarlockOrigins.setOrigin(player, OriginSlot.BLESSING, OriginIds.BLESSING_NONE);
        if (result != OriginSetResult.SUCCESS && result != OriginSetResult.UNCHANGED) {
            NewNewWarlockCore.LOGGER.debug("[origin] 无法为 {} 补空白加护：{}", player.getGameProfile().name(), result);
        }
    }

    public static void onServerStarted(ServerStartedEvent event) {
        validateLayers();
        validateConfig();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SNAPSHOTS.clear();
        tickCounter = 0;
    }

    private static void validateLayers() {
        OriginLayer race = LayerDataManager.INSTANCE.getLayer(OriginSlot.RACE.layerId());
        if (race == null) {
            NewNewWarlockCore.LOGGER.error("[origin] 种族层 {} 不存在，内置种族无法被覆盖", OriginSlot.RACE.layerId());
        } else {
            List<Identifier> available = race.getAvailableOriginIds();
            long leaked = available.stream()
                .filter(id -> "neoorigins".equals(id.getNamespace()))
                .count();
            if (leaked > 0) {
                NewNewWarlockCore.LOGGER.error(
                    "[origin] 种族层仍有 {} 个内置 neoorigins 起源，请检查数据包加载顺序或 replace 字段", leaked);
            }
        }

        OriginLayer clazz = LayerDataManager.INSTANCE.getLayer(Identifier.fromNamespaceAndPath("neoorigins", "class"));
        if (clazz != null && clazz.enabled()) {
            NewNewWarlockCore.LOGGER.error("[origin] class 层仍处于启用状态，内置职业会出现在选择界面");
        }

        if (LayerDataManager.INSTANCE.getLayer(OriginSlot.ESSENCE.layerId()) == null) {
            NewNewWarlockCore.LOGGER.error("[origin] 灵质层 {} 不存在", OriginSlot.ESSENCE.layerId());
        }

        OriginLayer blessing = LayerDataManager.INSTANCE.getLayer(OriginSlot.BLESSING.layerId());
        if (blessing == null) {
            NewNewWarlockCore.LOGGER.error("[origin] 加护层 {} 不存在", OriginSlot.BLESSING.layerId());
        } else if (!blessing.hidden()) {
            NewNewWarlockCore.LOGGER.warn("[origin] 加护层未标记 hidden，首次选择判定可能被它影响");
        }
    }

    private static void validateConfig() {
        if (GameplayConfig.isAutoHuman()) {
            NewNewWarlockCore.LOGGER.warn("[origin] NeoOrigins 的 auto_human 已开启，玩家会被强制赋予 neoorigins:human");
        }
        if (GameplayConfig.getRandomMode() != GameplayConfig.RandomMode.DISABLED) {
            NewNewWarlockCore.LOGGER.warn("[origin] NeoOrigins 的随机起源模式未关闭，首次选择可能自动抽取");
        }
        if (GameplayConfig.isSkipInitialSelection()) {
            NewNewWarlockCore.LOGGER.warn("[origin] NeoOrigins 的 skip_initial_selection 已开启，首次进入不会弹出选择界面");
        }
    }
}
