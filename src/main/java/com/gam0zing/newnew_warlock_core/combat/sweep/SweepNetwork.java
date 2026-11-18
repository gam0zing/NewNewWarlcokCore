package com.gam0zing.newnew_warlock_core.combat.sweep;

import com.gam0zing.newnew_warlock_core.combat.sweep.net.SweepSwingPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 空挥横扫的网络通道。 */
public final class SweepNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
            .playToServer(SweepSwingPayload.TYPE, SweepSwingPayload.STREAM_CODEC, SweepNetwork::handle);
    }

    private static void handle(SweepSwingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> SweepDispatcher.dispatchEmptySwing(player));
        }
    }

    private SweepNetwork() {}
}
