package com.gam0zing.newnew_warlock_core.skill.progress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommand;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

/**
 * 玩家身上的技能树进度，作为持久化附件保存。
 * <p>包含树级解锁状态、已解锁节点、以及已执行命令（用于撤销与重放）。
 * 点数上限仍在 {@code newnew_points} 属性上，已分配点数由这里的命令消耗求和得到，两者分开存储。</p>
 */
public final class SkillProgress {

    public static final Codec<SkillProgress> CODEC =
        Codec.unboundedMap(Identifier.CODEC, TreeProgress.CODEC)
            .xmap(SkillProgress::new, progress -> Map.copyOf(progress.trees));

    private final Map<Identifier, TreeProgress> trees;

    private SkillProgress(Map<Identifier, TreeProgress> trees) {
        this.trees = new LinkedHashMap<>(trees);
    }

    public SkillProgress() {
        this.trees = new LinkedHashMap<>();
    }

    /** {@return 玩家身上的进度附件} */
    public static SkillProgress of(Player player) {
        return player.getData(SkillProgressTypes.SKILL_PROGRESS);
    }

    public Map<Identifier, TreeProgress> trees() {
        return Collections.unmodifiableMap(this.trees);
    }

    /** 取或创建一棵树的进度 */
    public TreeProgress tree(Identifier treeId) {
        return this.trees.computeIfAbsent(treeId, key -> TreeProgress.empty());
    }

    @Nullable
    public TreeProgress treeOrNull(Identifier treeId) {
        return this.trees.get(treeId);
    }

    public boolean isTreeUnlocked(Identifier treeId) {
        TreeProgress progress = this.trees.get(treeId);
        return progress != null && progress.isUnlocked();
    }

    /** {@return 全部树已执行的命令，按树、按解锁顺序展开} */
    public List<PointAllocationCommand> allAppliedCommands() {
        List<PointAllocationCommand> commands = new ArrayList<>();
        for (TreeProgress progress : this.trees.values()) {
            for (AppliedNode node : progress.applied()) {
                commands.addAll(node.commands());
            }
        }
        return commands;
    }

    /** {@return 全部树已分配的点数之和，用于与 {@code newnew_points} 相减} */
    public double totalSpent() {
        double total = 0.0D;
        for (TreeProgress progress : this.trees.values()) {
            total += progress.spent();
        }
        return total;
    }
}
