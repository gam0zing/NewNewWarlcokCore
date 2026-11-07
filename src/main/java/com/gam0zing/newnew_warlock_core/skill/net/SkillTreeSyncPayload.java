package com.gam0zing.newnew_warlock_core.skill.net;

import java.util.List;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeDefinition;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端下发全部技能树定义（原始 JSON，客户端用同一解析器还原）。
 */
public record SkillTreeSyncPayload(List<Entry> trees) implements CustomPacketPayload {

    public static final Type<SkillTreeSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "skill_trees_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillTreeSyncPayload> STREAM_CODEC =
        StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SkillTreeSyncPayload::trees,
            SkillTreeSyncPayload::new);

    public SkillTreeSyncPayload {
        trees = List.copyOf(trees);
    }

    /** 一条树定义：id 决定树 id，json 是数据包原文 */
    public record Entry(Identifier id, String json) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
            StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::id,
                ByteBufCodecs.STRING_UTF8, Entry::json,
                Entry::new);
    }

    public static SkillTreeSyncPayload of(List<SkillTreeDefinition> definitions) {
        return new SkillTreeSyncPayload(
            definitions.stream().map(definition -> new Entry(definition.id(), definition.rawJson())).toList());
    }

    @Override
    public Type<SkillTreeSyncPayload> type() {
        return TYPE;
    }
}
