package com.gam0zing.newnew_warlock_core.mob.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 当前位置总难度（服务端 → 客户端）。
 * <p>由服务端算出完整难度后下发，客户端 HUD 直接显示；
 * {@code offset} 是服务器游戏难度带来的固定偏移（简单 -12 / 普通 0 / 困难 +12），仅用于显示。
 * {@code mediumThreshold}、{@code highThreshold} 是通用难度档位，用于 HUD 颜色判定，
 * 保证客户端与服务端配置一致。</p>
 */
public record DifficultySyncPayload(
    double difficulty, double offset, double mediumThreshold, double highThreshold)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DifficultySyncPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
            NewNewWarlockCore.MOD_ID, "difficulty_sync"));

    public static final StreamCodec<FriendlyByteBuf, DifficultySyncPayload> STREAM_CODEC =
        StreamCodec.of(DifficultySyncPayload::encode, DifficultySyncPayload::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, DifficultySyncPayload payload) {
        buffer.writeDouble(payload.difficulty());
        buffer.writeDouble(payload.offset());
        buffer.writeDouble(payload.mediumThreshold());
        buffer.writeDouble(payload.highThreshold());
    }

    private static DifficultySyncPayload decode(FriendlyByteBuf buffer) {
        return new DifficultySyncPayload(
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
