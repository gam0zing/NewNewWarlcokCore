package com.gam0zing.newnew_warlock_core.skill.tree;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

/**
 * 技能树的一个节点。
 *
 * @param id          树内唯一 id，必须匹配 {@code [a-z0-9_./-]+}
 * @param x           画布坐标 X
 * @param y           画布坐标 Y
 * @param name        节点展示名称
 * @param description 节点描述，悬停时展示
 * @param icon        图标物品 id，可为 null（只显示文字）
 * @param commands    解锁时要执行的命令
 */
public record SkillTreeNode(
    String id,
    float x,
    float y,
    String name,
    String description,
    @Nullable Identifier icon,
    List<NodeCommand> commands) {

    public SkillTreeNode {
        commands = List.copyOf(commands);
    }

    /** {@return 解锁该节点消耗的点数，等于所有命令消耗之和} */
    public double cost() {
        double total = 0.0D;
        for (NodeCommand command : this.commands) {
            total += command.command().cost();
        }
        return total;
    }
}
