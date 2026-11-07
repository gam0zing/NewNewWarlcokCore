package com.gam0zing.newnew_warlock_core.skill;

import java.util.ArrayList;
import java.util.List;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommand;
import com.gam0zing.newnew_warlock_core.skill.net.SkillProgressSyncPayload;
import com.gam0zing.newnew_warlock_core.skill.net.SkillTreeSyncPayload;
import com.gam0zing.newnew_warlock_core.skill.progress.AppliedNode;
import com.gam0zing.newnew_warlock_core.skill.progress.SkillProgress;
import com.gam0zing.newnew_warlock_core.skill.progress.TreeProgress;
import com.gam0zing.newnew_warlock_core.skill.tree.NodeCommand;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeDefinition;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeNode;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTrees;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 技能树的服务端业务：解锁、重置、登录恢复与同步。
 * <p>所有入口都只在服务端调用；客户端只发请求、渲染回执。</p>
 */
public final class SkillTreeService {

    private SkillTreeService() {}

    /** 登录/重生后：补默认开放的树、重放已应用命令、下发数据 */
    public static void onLogin(ServerPlayer player) {
        SkillProgress progress = SkillProgress.of(player);

        for (SkillTreeDefinition tree : SkillTrees.all().values()) {
            TreeProgress treeProgress = progress.treeOrNull(tree.id());
            if (treeProgress == null && !tree.defaultUnlocked()) {
                continue;
            }
            TreeProgress target = progress.tree(tree.id());
            if (tree.defaultUnlocked()) {
                target.unlockTree();
            }
            if (!target.isUnlocked()) {
                continue;
            }
            // 幂等重放：修复修饰符丢失等漂移
            for (AppliedNode applied : target.applied()) {
                for (PointAllocationCommand command : applied.commands()) {
                    command.doCommand(player);
                }
            }
        }

        syncAll(player);
    }

    /** 解锁一个节点 */
    public static void unlockNode(ServerPlayer player, Identifier treeId, String nodeId) {
        SkillTreeDefinition tree = SkillTrees.get(treeId);
        if (tree == null) {
            return;
        }
        SkillTreeNode node = tree.node(nodeId);
        if (node == null) {
            return;
        }

        SkillProgress progress = SkillProgress.of(player);
        TreeProgress treeProgress = progress.treeOrNull(treeId);
        if (treeProgress == null) {
            if (!tree.defaultUnlocked()) {
                return;
            }
            treeProgress = progress.tree(treeId);
            treeProgress.unlockTree();
        }
        if (!treeProgress.isUnlocked() || treeProgress.isNodeUnlocked(nodeId)) {
            syncAll(player);
            return;
        }

        // 前置：没有前置（根节点）或至少一个前置已解锁
        List<SkillTreeNode> prerequisites = tree.prerequisitesOf(nodeId);
        TreeProgress unlockedState = treeProgress;
        if (!prerequisites.isEmpty()
            && prerequisites.stream().noneMatch(prerequisite -> unlockedState.isNodeUnlocked(prerequisite.id()))) {
            return;
        }

        // 点数：跨树共享，按全部已应用命令计算
        double remaining = PointAllocationCommand.remaining(player, progress.allAppliedCommands());
        if (node.cost() > remaining) {
            return;
        }

        List<PointAllocationCommand> applied = new ArrayList<>(node.commands().size());
        for (NodeCommand nodeCommand : node.commands()) {
            if (!nodeCommand.command().doCommand(player)) {
                for (int index = applied.size() - 1; index >= 0; index--) {
                    applied.get(index).undo(player);
                }
                return;
            }
            applied.add(nodeCommand.command());
        }
        treeProgress.addNode(new AppliedNode(nodeId, applied));

        syncAll(player);
    }

    /** 重置一棵树：逆序撤销全部命令，按已分配点数收取经验 */
    public static void resetTree(ServerPlayer player, Identifier treeId) {
        SkillTreeDefinition tree = SkillTrees.get(treeId);
        SkillProgress progress = SkillProgress.of(player);
        TreeProgress treeProgress = progress.treeOrNull(treeId);
        if (tree == null || treeProgress == null) {
            return;
        }

        int xpCost = resetXpCost(tree, treeProgress.spent());
        if (xpCost > 0 && player.experienceLevel < xpCost) {
            return;
        }

        List<AppliedNode> applied = treeProgress.applied();
        for (int nodeIndex = applied.size() - 1; nodeIndex >= 0; nodeIndex--) {
            List<PointAllocationCommand> commands = applied.get(nodeIndex).commands();
            for (int commandIndex = commands.size() - 1; commandIndex >= 0; commandIndex--) {
                commands.get(commandIndex).undo(player);
            }
        }
        treeProgress.clearNodes();

        if (xpCost > 0) {
            player.giveExperienceLevels(-xpCost);
        }

        syncAll(player);
    }

    /** {@return 重置该树需要消耗的经验等级} */
    public static int resetXpCost(SkillTreeDefinition tree, double spentPoints) {
        if (tree.resetXpPerPoint() <= 0.0D) {
            return 0;
        }
        return (int) Math.ceil(spentPoints * tree.resetXpPerPoint());
    }

    /** 下发全部树定义与玩家进度 */
    public static void syncAll(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SkillTreeSyncPayload.of(List.copyOf(SkillTrees.all().values())));
        PacketDistributor.sendToPlayer(player, SkillProgressSyncPayload.of(SkillProgress.of(player)));
    }

    /** 资源重载后只下发树定义 */
    public static void syncDefinitions(ServerPlayer player) {
        List<SkillTreeDefinition> definitions = List.copyOf(SkillTrees.all().values());
        PacketDistributor.sendToPlayer(player, SkillTreeSyncPayload.of(definitions));
        NewNewWarlockCore.LOGGER.debug("[skill] 已向 {} 下发 {} 棵技能树定义", player.getGameProfile().name(),
            definitions.size());
    }
}
