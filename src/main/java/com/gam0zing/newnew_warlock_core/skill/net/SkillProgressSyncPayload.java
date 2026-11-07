package com.gam0zing.newnew_warlock_core.skill.net;

import java.util.ArrayList;
import java.util.List;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.skill.progress.SkillProgress;
import com.gam0zing.newnew_warlock_core.skill.progress.TreeProgress;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端下发的玩家技能树进度（界面只需要展示数据，不包含可执行命令）。
 */
public record SkillProgressSyncPayload(List<TreeView> trees, double totalSpent) implements CustomPacketPayload {

    public static final Type<SkillProgressSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "skill_progress_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillProgressSyncPayload> STREAM_CODEC =
        StreamCodec.composite(
            TreeView.STREAM_CODEC.apply(ByteBufCodecs.list()), SkillProgressSyncPayload::trees,
            ByteBufCodecs.DOUBLE, SkillProgressSyncPayload::totalSpent,
            SkillProgressSyncPayload::new);

    public SkillProgressSyncPayload {
        trees = List.copyOf(trees);
    }

    /**
     * 单棵树的展示状态。
     *
     * @param treeId   树 id
     * @param unlocked 树是否已对玩家开放
     * @param nodes    已解锁节点 id
     * @param spent    该树已分配点数
     */
    public record TreeView(Identifier treeId, boolean unlocked, List<String> nodes, double spent) {

        public static final StreamCodec<RegistryFriendlyByteBuf, TreeView> STREAM_CODEC =
            StreamCodec.composite(
                Identifier.STREAM_CODEC, TreeView::treeId,
                ByteBufCodecs.BOOL, TreeView::unlocked,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TreeView::nodes,
                ByteBufCodecs.DOUBLE, TreeView::spent,
                TreeView::new);

        public TreeView {
            nodes = List.copyOf(nodes);
        }
    }

    public static SkillProgressSyncPayload of(SkillProgress progress) {
        List<TreeView> views = new ArrayList<>();
        progress.trees().forEach((treeId, treeProgress) -> views.add(toView(treeId, treeProgress)));
        return new SkillProgressSyncPayload(views, progress.totalSpent());
    }

    private static TreeView toView(Identifier treeId, TreeProgress progress) {
        return new TreeView(treeId, progress.isUnlocked(), List.copyOf(progress.nodeIds()), progress.spent());
    }

    @Override
    public Type<SkillProgressSyncPayload> type() {
        return TYPE;
    }
}
