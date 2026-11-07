package com.gam0zing.newnew_warlock_core.skill.command;

/**
 * 命令参数的取值类型。
 * <p>只支持基本类型，编辑期工具（网页）在此基础上通过 enum 表提供下拉辅助，
 * 但运行时数据结构只认这些基本类型。</p>
 */
public enum ParamType {
    STRING("string"),
    INT("int"),
    FLOAT("float"),
    BOOL("bool");

    private final String id;

    ParamType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}
