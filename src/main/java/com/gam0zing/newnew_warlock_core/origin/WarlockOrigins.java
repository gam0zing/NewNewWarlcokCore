package com.gam0zing.newnew_warlock_core.origin;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * 起源系统的公共门面。
 * <p>其他模块只通过这里读写玩家的种族/灵质/加护，不直接引用 NeoOrigins。
 * 未安装 NeoOrigins 时 {@link #isAvailable()} 返回 false，所有写操作返回
 * {@link OriginSetResult#UNAVAILABLE}。</p>
 */
public final class WarlockOrigins {

    @Nullable
    private static volatile OriginAccess access;

    private WarlockOrigins() {}

    /** 由兼容层在 NeoOrigins 存在时调用；重复调用会覆盖实现。 */
    public static void install(OriginAccess originAccess) {
        access = originAccess;
        NewNewWarlockCore.LOGGER.info("[origin] 已接入 NeoOrigins 起源后端");
    }

    /** {@return 是否安装了 NeoOrigins 兼容后端} */
    public static boolean isAvailable() {
        return access != null;
    }

    /** {@return 玩家三个槽位的当前起源，缺失的槽位不出现在结果中} */
    public static Map<OriginSlot, Identifier> snapshot(ServerPlayer player) {
        OriginAccess current = access;
        if (current == null) {
            return Collections.emptyMap();
        }
        Map<Identifier, Identifier> raw = current.snapshot(player);
        Map<OriginSlot, Identifier> result = new EnumMap<>(OriginSlot.class);
        for (OriginSlot slot : OriginSlot.values()) {
            Identifier origin = raw.get(slot.layerId());
            if (origin != null) {
                result.put(slot, origin);
            }
        }
        return result;
    }

    /** {@return 玩家某个槽位的起源} */
    public static Optional<Identifier> getOrigin(ServerPlayer player, OriginSlot slot) {
        return Optional.ofNullable(snapshot(player).get(slot));
    }

    /** {@return 玩家是否在任意一个槽位拥有该起源} */
    public static boolean hasOrigin(ServerPlayer player, Identifier origin) {
        return snapshot(player).containsValue(origin);
    }

    /** 设置某个槽位的起源；也用于把玩家替换为无法直接选择的起源。 */
    public static OriginSetResult setOrigin(ServerPlayer player, OriginSlot slot, Identifier origin) {
        OriginAccess current = access;
        if (current == null) {
            return OriginSetResult.UNAVAILABLE;
        }
        return current.setOrigin(player, slot.layerId(), origin);
    }

    /** {@return 是否成功清除} */
    public static boolean clearOrigin(ServerPlayer player, OriginSlot slot) {
        OriginAccess current = access;
        return current != null && current.clearOrigin(player, slot.layerId());
    }

    /** 打开单层重选界面；未安装 NeoOrigins 时返回 false。 */
    public static boolean openPicker(
        ServerPlayer player,
        List<OriginSlot> slots,
        @Nullable Item consumeItem,
        String messageKey) {
        OriginAccess current = access;
        if (current == null || slots.isEmpty()) {
            return false;
        }
        List<Identifier> layers = slots.stream().map(OriginSlot::layerId).toList();
        return current.openPicker(player, layers, consumeItem, messageKey);
    }
}
