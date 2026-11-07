package com.gam0zing.newnew_warlock_core.skill.tree;

import java.util.List;

import com.gam0zing.newnew_warlock_core.skill.command.ParamValues;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommand;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommandType;

/**
 * 节点内的一条已构造命令：类型定义、原始参数、可执行实例。
 * <p>保留参数是为了界面展示 {@code summary}；保留实例是为了执行与撤销不再依赖数据包解析。</p>
 */
public record NodeCommand(
    PointAllocationCommandType<?> type,
    ParamValues params,
    PointAllocationCommand command) {

    /** {@return 参数替换后的玩家可见简述} */
    public String renderedSummary() {
        return this.type.renderSummary(this.params);
    }
}
