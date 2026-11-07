package com.gam0zing.newnew_warlock_core.skill.command;

import org.jetbrains.annotations.Nullable;

/**
 * 命令参数的声明：名称、类型、默认值。
 * <p>默认值用于网页工具新建节点时的初始参数，运行时缺失参数时也回退到默认值。</p>
 *
 * @param name         参数名，与命令工厂读取的键一致
 * @param type         参数类型
 * @param defaultValue 默认值，类型必须与 {@code type} 对应
 */
public record ParamSpec(String name, ParamType type, @Nullable Object defaultValue) {

    public ParamSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("param name must not be blank");
        }
        switch (type) {
            case STRING -> {
                if (defaultValue != null && !(defaultValue instanceof String)) {
                    throw new IllegalArgumentException("default of '" + name + "' must be a string");
                }
            }
            case INT -> {
                if (defaultValue != null && !(defaultValue instanceof Integer)) {
                    throw new IllegalArgumentException("default of '" + name + "' must be an int");
                }
            }
            case FLOAT -> {
                if (defaultValue != null && !(defaultValue instanceof Double)) {
                    throw new IllegalArgumentException("default of '" + name + "' must be a float");
                }
            }
            case BOOL -> {
                if (defaultValue != null && !(defaultValue instanceof Boolean)) {
                    throw new IllegalArgumentException("default of '" + name + "' must be a bool");
                }
            }
        }
    }

    public static ParamSpec string(String name, String defaultValue) {
        return new ParamSpec(name, ParamType.STRING, defaultValue);
    }

    public static ParamSpec integer(String name, int defaultValue) {
        return new ParamSpec(name, ParamType.INT, defaultValue);
    }

    public static ParamSpec floating(String name, double defaultValue) {
        return new ParamSpec(name, ParamType.FLOAT, defaultValue);
    }

    public static ParamSpec bool(String name, boolean defaultValue) {
        return new ParamSpec(name, ParamType.BOOL, defaultValue);
    }
}
