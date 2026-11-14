package com.gam0zing.newnew_warlock_core.mob.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 玩家击杀生物获得的属性点（服务端 → 击杀者客户端）。
 * <p>{@code gained} 是本次获得的点数，{@code scale} 是双方 points 比较得出的缩放，
 * 客户端 HUD 用缩放决定提示颜色（大于 1 金色、等于 1 白色、小于 1 灰色）。</p>
 */
public record MobKillPointsPayload(double gained, double scale) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MobKillPointsPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
            NewNewWarlockCore.MOD_ID, "mob_kill_points"));

    public static final StreamCodec<FriendlyByteBuf, MobKillPointsPayload> STREAM_CODEC =
        StreamCodec.of(MobKillPointsPayload::encode, MobKillPointsPayload::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, MobKillPointsPayload payload) {
        buffer.writeDouble(payload.gained());
        buffer.writeDouble(payload.scale());
    }

    private static MobKillPointsPayload decode(FriendlyByteBuf buffer) {
        return new MobKillPointsPayload(buffer.readDouble(), buffer.readDouble());
    }
}
