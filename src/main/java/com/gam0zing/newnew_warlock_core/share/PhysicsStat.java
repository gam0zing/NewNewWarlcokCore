package com.gam0zing.newnew_warlock_core.share;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/**
 * 物理档案里的一个可修饰数值（武器倍率、护甲厚度等）。
 * <p>语义与 {@code Attribute} 一致：{@link #base()} 是基准值，{@link #modifiers()} 记录可叠加的修正，
 * {@link #value()} 按原版属性的运算顺序求值：</p>
 * <ol>
 *     <li>{@link Operation#ADD_VALUE}：总量加上 {@code amount}</li>
 *     <li>{@link Operation#ADD_MULTIPLIED_BASE}：总量加上 {@code base * amount}</li>
 *     <li>{@link Operation#ADD_MULTIPLIED_TOTAL}：总量乘以 {@code 1 + amount}</li>
 * </ol>
 * <p>这样装备档案可以像属性一样被外部（数据包、词条、附魔等）追加修正，而不必改动基准值。</p>
 *
 * <p>JSON 结构：{@code {"base": 1.0, "modifiers": [{"id": "...", "amount": 0.2, "operation": "add_value"}]}}。
 * 只写一个数字时请使用 {@code "xxx": {"base": ...}} 的写法；数值字段本身不做纯数字兼容。</p>
 *
 * @param base      基准值
 * @param modifiers 修正列表（顺序不影响结果，按操作类型分组求值）
 */
public record PhysicsStat(double base, List<AttributeModifier> modifiers) {

    /** 基准值 0、无修正 */
    public static final PhysicsStat ZERO = new PhysicsStat(0.0D, List.of());

    /** 基准值 1、无修正 */
    public static final PhysicsStat ONE = new PhysicsStat(1.0D, List.of());

    public static final Codec<PhysicsStat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.optionalFieldOf("base", 0.0D).forGetter(PhysicsStat::base),
        AttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(PhysicsStat::modifiers))
        .apply(instance, PhysicsStat::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsStat> STREAM_CODEC =
        ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public PhysicsStat {
        base = Double.isFinite(base) ? base : 0.0D;
        modifiers = List.copyOf(modifiers);
    }

    /** {@return 只有基准值、没有修正的档案} */
    public static PhysicsStat of(double base) {
        return new PhysicsStat(base, List.of());
    }

    /** {@return 按原版属性运算顺序求值后的当前值} */
    public double value() {
        double total = this.base;
        for (AttributeModifier modifier : this.modifiers) {
            if (modifier.operation() == Operation.ADD_VALUE) {
                total += modifier.amount();
            }
        }
        for (AttributeModifier modifier : this.modifiers) {
            if (modifier.operation() == Operation.ADD_MULTIPLIED_BASE) {
                total += this.base * modifier.amount();
            }
        }
        for (AttributeModifier modifier : this.modifiers) {
            if (modifier.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
                total *= 1.0D + modifier.amount();
            }
        }
        return Double.isFinite(total) ? total : 0.0D;
    }

    /** {@return 求值后按非负下限截断的值} */
    public double nonNegativeValue() {
        return Math.max(0.0D, this.value());
    }
}
