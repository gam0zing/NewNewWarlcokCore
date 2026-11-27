package com.gam0zing.newnew_warlock_core.compat.neoorigins;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.cyberday1.neoorigins.api.NeoOriginsAPI;
import com.cyberday1.neoorigins.api.origin.SetOriginResult;
import com.cyberday1.neoorigins.data.OriginDataManager;
import com.cyberday1.neoorigins.network.NeoOriginsNetwork;
import com.gam0zing.newnew_warlock_core.origin.OriginAccess;
import com.gam0zing.newnew_warlock_core.origin.OriginSetResult;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * NeoOrigins 后端实现。只在 {@link NeoOriginsGate#isLoaded()} 为 true 时实例化。
 */
public final class NeoOriginsOriginAccess implements OriginAccess {

    @Override
    public Map<Identifier, Identifier> snapshot(ServerPlayer player) {
        return NeoOriginsAPI.getOrigins(player);
    }

    @Override
    public OriginSetResult setOrigin(ServerPlayer player, Identifier layer, Identifier origin) {
        return convert(NeoOriginsAPI.setOrigin(player, layer, origin));
    }

    @Override
    public boolean clearOrigin(ServerPlayer player, Identifier layer) {
        return NeoOriginsAPI.clearOrigin(player, layer);
    }

    /** 复合子能力由 {@code NeoOriginsAPIHasPowerMixin} 在 API 层展开，这里不再重复查询。 */
    @Override
    public boolean hasPower(ServerPlayer player, Identifier power) {
        return NeoOriginsAPI.hasPower(player, power);
    }

    @Override
    public boolean originExists(Identifier origin) {
        return OriginDataManager.INSTANCE.hasOrigin(origin);
    }

    @Override
    public boolean openPicker(
        ServerPlayer player,
        List<Identifier> layers,
        @Nullable Item consumeItem,
        String messageKey) {
        // 提交时消耗物品、取消时回滚旧起源，由 NeoOrigins 自己处理
        NeoOriginsNetwork.beginLayerPicker(player, layers, true, 0, consumeItem, messageKey);
        return true;
    }

    private static OriginSetResult convert(SetOriginResult result) {
        return switch (result) {
            case SUCCESS -> OriginSetResult.SUCCESS;
            case UNCHANGED -> OriginSetResult.UNCHANGED;
            case UNKNOWN_LAYER -> OriginSetResult.UNKNOWN_LAYER;
            case UNKNOWN_ORIGIN -> OriginSetResult.UNKNOWN_ORIGIN;
            case CLAIMED -> OriginSetResult.CLAIMED;
            case CANCELLED -> OriginSetResult.CANCELLED;
        };
    }
}
