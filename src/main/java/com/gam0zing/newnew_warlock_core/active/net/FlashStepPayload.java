package com.gam0zing.newnew_warlock_core.active.net;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 闪身步请求：客户端把双击到的方向上报给服务端，由服务端做全部判定。
 *
 * @param direction 0 前、1 后、2 左、3 右
 */
public record FlashStepPayload(byte direction) implements CustomPacketPayload {

    public static final Type<FlashStepPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "flash_step"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlashStepPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BYTE, FlashStepPayload::direction,
            FlashStepPayload::new);

    @Override
    public Type<FlashStepPayload> type() {
        return TYPE;
    }
}
