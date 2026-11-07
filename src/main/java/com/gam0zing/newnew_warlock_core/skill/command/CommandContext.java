package com.gam0zing.newnew_warlock_core.skill.command;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;

/**
 * 命令在技能树中的位置，供工厂生成确定性的内部标识。
 * <p>修饰符 id 由树 id、节点 id、命令序号推导，因此同一节点每次构造出的命令完全一致：
 * 重放幂等，撤销也不需要额外记录。</p>
 *
 * @param treeId 技能树 id
 * @param nodeId 树内的节点 id
 * @param index  命令在该节点内的序号
 */
public record CommandContext(Identifier treeId, String nodeId, int index) {

    public CommandContext {
        if (treeId == null) {
            throw new IllegalArgumentException("treeId must not be null");
        }
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
    }

    /** {@return 由位置推导出的稳定修饰符 id} */
    public Identifier modifierId() {
        return Identifier.fromNamespaceAndPath(
            NewNewWarlockCore.MOD_ID,
            "skill/" + this.treeId.getPath() + "/" + this.nodeId + "/" + this.index);
    }
}
