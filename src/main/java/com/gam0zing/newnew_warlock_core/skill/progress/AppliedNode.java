package com.gam0zing.newnew_warlock_core.skill.progress;

import java.util.List;

import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommand;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommands;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 玩家已解锁的一个节点及其已执行命令。
 * <p>存实例而不是只存节点 id，是为了数据包更新后撤销与重放仍然精确。</p>
 */
public record AppliedNode(String nodeId, List<PointAllocationCommand> commands) {

    public static final Codec<AppliedNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("node").forGetter(AppliedNode::nodeId),
        PointAllocationCommands.CODEC.listOf().fieldOf("commands").forGetter(AppliedNode::commands)
    ).apply(instance, AppliedNode::new));

    public AppliedNode {
        commands = List.copyOf(commands);
    }

    /** {@return 该节点消耗的点数} */
    public double cost() {
        double total = 0.0D;
        for (PointAllocationCommand command : this.commands) {
            total += command.cost();
        }
        return total;
    }
}
