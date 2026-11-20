package com.gam0zing.newnew_warlock_core.skill.unlock;

import java.util.List;
import java.util.function.Function;

import com.gam0zing.newnew_warlock_core.skill.command.ParamSpec;
import com.gam0zing.newnew_warlock_core.skill.command.ParamValues;

import net.minecraft.resources.Identifier;

/**
 * 简单的解锁方式类型实现，用工厂函数把参数转成解锁条件。
 */
public record SimpleTreeUnlockMode(
    Identifier id,
    String displayName,
    String documentation,
    List<ParamSpec> params,
    Function<ParamValues, TreeUnlock> factory) implements TreeUnlockMode {

    public SimpleTreeUnlockMode {
        params = List.copyOf(params);
    }

    @Override
    public TreeUnlock create(ParamValues values) {
        return this.factory.apply(values);
    }
}
