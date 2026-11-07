package com.gam0zing.newnew_warlock_core.skill.command;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;

/**
 * 命令类型的完整定义：序列化 codec、编辑期元数据、参数声明与构造工厂。
 * <p>注册表里存的就是它，定义表导出、数据包校验、节点构造都从同一份数据出发。</p>
 *
 * @param <T> 命令实现类型
 */
public final class PointAllocationCommandType<T extends PointAllocationCommand> {

    /**
     * 由参数构造命令实例。
     * <p>实现可以在此生成内部字段（如修饰符 id），但只能使用 {@link CommandContext} 提供的稳定信息。</p>
     */
    @FunctionalInterface
    public interface Factory<T> {
        T create(ParamValues values, CommandContext context) throws IllegalArgumentException;
    }

    private final Identifier id;
    private final MapCodec<T> codec;
    private final String displayName;
    private final String documentation;
    private final String summary;
    private final List<ParamSpec> params;
    private final Factory<T> factory;

    /**
     * @param id          类型注册键
     * @param codec       命令实例的持久化 codec
     * @param displayName 编辑期展示名
     * @param documentation 编辑期文档
     * @param summary     玩家可见的简述，可包含 {@code {参数名}} 占位符
     * @param params      用户可编辑的参数声明
     * @param factory     参数 → 命令实例
     */
    public PointAllocationCommandType(
        Identifier id,
        MapCodec<T> codec,
        String displayName,
        String documentation,
        String summary,
        List<ParamSpec> params,
        Factory<T> factory) {
        this.id = id;
        this.codec = codec;
        this.displayName = displayName;
        this.documentation = documentation;
        this.summary = summary;
        this.params = List.copyOf(params);
        this.factory = factory;
    }

    public Identifier id() {
        return this.id;
    }

    public MapCodec<T> codec() {
        return this.codec;
    }

    public String displayName() {
        return this.displayName;
    }

    public String documentation() {
        return this.documentation;
    }

    public String summary() {
        return this.summary;
    }

    public List<ParamSpec> params() {
        return this.params;
    }

    /**
     * 构造命令实例。
     *
     * @throws IllegalArgumentException 参数非法（如未知的属性 id）
     */
    public T create(ParamValues values, CommandContext context) {
        return this.factory.create(values, context);
    }

    /**
     * 用参数替换 {@link #summary()} 里的 {@code {参数名}} 占位符。
     * <p>仅用于界面展示，不做格式化与本地化。</p>
     */
    public String renderSummary(@Nullable ParamValues values) {
        String rendered = this.summary;
        if (values == null) {
            return rendered;
        }
        for (ParamSpec spec : this.params) {
            rendered = rendered.replace("{" + spec.name() + "}", String.valueOf(values.get(spec.name())));
        }
        return rendered;
    }
}
