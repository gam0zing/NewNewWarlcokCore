package com.gam0zing.newnew_warlock_core.skill.command;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.registry.ModAttributes;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * 点数分配命令基类。
 * <p>命令表示“花掉若干点数、产生某种效果”的一次操作，效果由子类按自身业务决定，不局限于修饰属性。
 * 它本身不依赖技能树结构：可以独立构造、独立编码/解码、独立执行与撤销。</p>
 *
 * <p><b>点数的口径：</b>点数上限是实体身上的 {@code newnew_points} 属性，命令不会修改它。
 * 已分配点数 = 已应用命令的 {@link #cost()} 之和，剩余点数 = 上限 − 已分配。
 * 因此命令只需要携带自己的消耗值，撤销时把该命令从集合中移除即可，无需遍历属性的修饰符。</p>
 *
 * <p>能不能负担由调用方判断（{@link #remaining} 与 {@link #cost()}），{@link #doCommand} 只负责执行，
 * 失败时不得留下任何副作用。</p>
 */
public abstract class PointAllocationCommand {

    private final double cost;

    /**
     * @param cost 本命令消耗的点数，必须为有限且非负的值
     */
    protected PointAllocationCommand(double cost) {
        if (Double.isNaN(cost) || Double.isInfinite(cost) || cost < 0.0D) {
            throw new IllegalArgumentException("cost must be a finite value >= 0, but was " + cost);
        }
        this.cost = cost;
    }

    /** {@return 本命令消耗的点数} */
    public final double cost() {
        return this.cost;
    }

    /** {@return 本命令的类型注册键，编码时写入 {@code type} 字段} */
    public abstract Identifier type();

    /** {@return 本命令的序列化 codec} */
    public abstract MapCodec<? extends PointAllocationCommand> codec();

    /**
     * 执行命令。
     *
     * @return 是否成功；返回 false 时必须不留下任何副作用
     */
    public abstract boolean doCommand(ServerPlayer player);

    /** 撤销命令，必须与 {@link #doCommand} 完全对称。 */
    public abstract void undo(ServerPlayer player);

    /**
     * 统计已分配点数。
     *
     * @param applied 已应用的命令集合，可以为空集合
     */
    public static double spent(Iterable<? extends PointAllocationCommand> applied) {
        double total = 0.0D;
        for (PointAllocationCommand command : applied) {
            total += command.cost();
        }
        return total;
    }

    /**
     * 计算剩余可分配点数 = {@code newnew_points} 当前值 − 已分配点数。
     *
     * @param entity  点数上限的来源；为 null 或没有该属性时视为 0
     * @param applied 已应用的命令集合，可以为空集合
     */
    public static double remaining(@Nullable LivingEntity entity, Iterable<? extends PointAllocationCommand> applied) {
        AttributeInstance points = entity == null ? null : entity.getAttribute(ModAttributes.NEWNEW_POINTS);
        return points == null ? 0.0D : points.getValue() - spent(applied);
    }
}
