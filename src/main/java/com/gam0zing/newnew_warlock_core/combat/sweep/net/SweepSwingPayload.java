package com.gam0zing.newnew_warlock_core.combat.sweep.net;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 空挥请求：客户端左键点击空气时上报，由服务端确认是否满足横扫条件。
 * <p>无字段，所有判定都在服务端完成。</p>
 */
public record SweepSwingPayload() implements CustomPacketPayload {

    public static final Type<SweepSwingPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "sweep_swing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SweepSwingPayload> STREAM_CODEC =
        StreamCodec.unit(new SweepSwingPayload());

    @Override
    public Type<SweepSwingPayload> type() {
        return TYPE;
    }
}
