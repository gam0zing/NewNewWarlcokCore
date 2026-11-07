package com.gam0zing.newnew_warlock_core.enchant.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

/**
 * 服务端基准快照（方案 B）：登录/数据包重载后由服务端下发，客户端用它替换本地 common 表，
 * 保证多人游戏里客户端显示的附魔等级上限与服务端判定一致。
 * <p>只传「基准值」与全局标量，不含玩家相关修正——那些规则只在服务端按玩家计算。</p>
 */
public record EnchantBaselineSyncPayload(
    List<EnchantBaseline.Entry> entries,
    EnchantBaselineConfig.GlobalSettings settings,
    int schemaVersion) implements CustomPacketPayload {

    /** 当前快照格式版本，后续加字段时递增并保证向后兼容。 */
    public static final int SCHEMA_VERSION = 1;

    /** 单次快照允许的最大条数，防畸形数据包。 */
    public static final int MAX_ENTRIES = 4096;

    public static final CustomPacketPayload.Type<EnchantBaselineSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
            NewNewWarlockCore.MOD_ID, "enchant_baseline_sync"));

    public static final StreamCodec<FriendlyByteBuf, EnchantBaselineSyncPayload> STREAM_CODEC =
        StreamCodec.of(EnchantBaselineSyncPayload::encode, EnchantBaselineSyncPayload::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, EnchantBaselineSyncPayload payload) {
        buffer.writeVarInt(payload.schemaVersion());

        EnchantBaselineConfig.GlobalSettings settings = payload.settings();
        buffer.writeVarInt(settings.maxPower());
        buffer.writeDouble(settings.xpCostScale());
        buffer.writeVarInt(settings.extraPower());
        buffer.writeVarInt(settings.anvilMaxCost());

        buffer.writeVarInt(payload.entries().size());
        for (EnchantBaseline.Entry entry : payload.entries()) {
            buffer.writeIdentifier(entry.id());
            buffer.writeVarInt(entry.maxLevel());
            buffer.writeVarInt(entry.weight());
            buffer.writeVarInt(entry.minCostBase());
            buffer.writeVarInt(entry.minCostPerLevelAboveFirst());
            buffer.writeVarInt(entry.maxCostBase());
            buffer.writeVarInt(entry.maxCostPerLevelAboveFirst());
            buffer.writeVarInt(entry.anvilCost());
            buffer.writeBoolean(entry.inTable());
            buffer.writeBoolean(entry.treasure());
            buffer.writeBoolean(entry.tradeable());
            buffer.writeBoolean(entry.onRandomLoot());
            buffer.writeBoolean(entry.onTradedEquipment());
        }
    }

    private static EnchantBaselineSyncPayload decode(FriendlyByteBuf buffer) {
        int schemaVersion = buffer.readVarInt();
        if (schemaVersion != SCHEMA_VERSION) {
            // 版本不一致时格式可能已变，继续解析会读出无意义的数据，直接拒绝比静默错读安全。
            throw new IllegalArgumentException("不支持的附魔基准快照版本: " + schemaVersion);
        }
        EnchantBaselineConfig.GlobalSettings settings = new EnchantBaselineConfig.GlobalSettings(
            buffer.readVarInt(), buffer.readDouble(), buffer.readVarInt(), buffer.readVarInt());

        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("非法的附魔基准快照条数: " + size);
        }

        List<EnchantBaseline.Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new EnchantBaseline.Entry(
                buffer.readIdentifier(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()));
        }

        return new EnchantBaselineSyncPayload(entries, settings, schemaVersion);
    }
}
