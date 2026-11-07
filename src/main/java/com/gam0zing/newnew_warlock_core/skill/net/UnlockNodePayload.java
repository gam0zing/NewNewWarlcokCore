package com.gam0zing.newnew_warlock_core.skill.net;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端请求解锁某个节点。
 */
public record UnlockNodePayload(Identifier treeId, String nodeId) implements CustomPacketPayload {

    public static final Type<UnlockNodePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "unlock_skill_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockNodePayload> STREAM_CODEC =
        StreamCodec.composite(
            Identifier.STREAM_CODEC, UnlockNodePayload::treeId,
            ByteBufCodecs.STRING_UTF8, UnlockNodePayload::nodeId,
            UnlockNodePayload::new);

    @Override
    public Type<UnlockNodePayload> type() {
        return TYPE;
    }
}
