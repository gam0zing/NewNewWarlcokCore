package com.gam0zing.newnew_warlock_core.skill.net;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端请求重置一棵技能树（消耗经验，回退该树全部已分配点数）。
 */
public record ResetTreePayload(Identifier treeId) implements CustomPacketPayload {

    public static final Type<ResetTreePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "reset_skill_tree"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResetTreePayload> STREAM_CODEC =
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ResetTreePayload::treeId,
            ResetTreePayload::new);

    @Override
    public Type<ResetTreePayload> type() {
        return TYPE;
    }
}
