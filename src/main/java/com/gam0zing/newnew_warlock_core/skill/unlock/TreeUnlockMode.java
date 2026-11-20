package com.gam0zing.newnew_warlock_core.skill.unlock;

import java.util.List;

import com.gam0zing.newnew_warlock_core.skill.command.ParamSpec;
import com.gam0zing.newnew_warlock_core.skill.command.ParamValues;

import net.minecraft.resources.Identifier;

/**
 * 解锁方式类型：注册表里的元素，负责声明参数并构造 {@link TreeUnlock}。
 * <p>与点数分配命令类型一样，定义表导出、数据包校验与实例构造共用这一份定义。</p>
 */
public interface TreeUnlockMode {

    /** {@return 类型注册名} */
    Identifier id();

    /** {@return 编辑器展示名} */
    String displayName();

    /** {@return 编辑器文档} */
    String documentation();

    /** {@return 用户可编辑的参数声明} */
    List<ParamSpec> params();

    /**
     * 由参数构造解锁条件。
     *
     * @throws IllegalArgumentException 参数非法
     */
    TreeUnlock create(ParamValues values);
}
