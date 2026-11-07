package com.gam0zing.newnew_warlock_core.skill.command;

import java.util.List;
import java.util.Objects;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 属性命令：消耗点数，给指定属性加一条 {@link AttributeModifier}。
 * <p>只修改属性修饰符，不触碰 {@code newnew_points}；修饰符使用永久修饰符，随实体属性 NBT 持久化，
 * 且以同一个 id 替换写入，因此重复执行是幂等的。</p>
 * <p>消耗、数值与修饰方式都来自参数，命令本身不写死任何数值。编辑期 {@code attribute} 是字符串（属性注册名），
 * 由工厂解析成 {@link Holder}；{@code operation} 同样以字符串声明（{@code add_value}、
 * {@code add_multiplied_base}、{@code add_multiplied_total}），由工厂解析成 {@link AttributeModifier.Operation}；
 * 修饰符 id 由命令在树内的位置推导，不出现在用户参数里。</p>
 */
public final class AttributeCommand extends PointAllocationCommand {

    public static final Identifier TYPE_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "attribute");

    /** 修饰方式：直接加减数值 */
    public static final String OP_ADD_VALUE = "add_value";
    /** 修饰方式：叠加在基础值上（{@code base * amount}） */
    public static final String OP_ADD_MULTIPLIED_BASE = "add_multiplied_base";
    /** 修饰方式：作用在最终值上（{@code total * (1 + amount)}） */
    public static final String OP_ADD_MULTIPLIED_TOTAL = "add_multiplied_total";

    private static final Codec<AttributeModifier.Operation> OPERATION_CODEC = Codec.STRING.flatXmap(
        text -> {
            try {
                return DataResult.success(parseOperation(text));
            } catch (IllegalArgumentException exception) {
                return DataResult.error(exception::getMessage);
            }
        },
        operation -> DataResult.success(operationName(operation)));

    public static final MapCodec<AttributeCommand> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(AttributeCommand::attribute),
        Codec.DOUBLE.fieldOf("gain").forGetter(AttributeCommand::gain),
        OPERATION_CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
            .forGetter(AttributeCommand::operation),
        Codec.DOUBLE.fieldOf("cost").forGetter(PointAllocationCommand::cost),
        Identifier.CODEC.fieldOf("modifier").forGetter(AttributeCommand::modifierId)
    ).apply(instance, AttributeCommand::new));

    /** 命令类型定义：定义表导出、参数校验、节点构造共用 */
    public static final PointAllocationCommandType<AttributeCommand> TYPE = new PointAllocationCommandType<>(
        TYPE_ID,
        CODEC,
        "属性",
        "给目标属性添加一条永久修饰符，可重复执行而不叠加。operation 为修饰方式："
            + OP_ADD_VALUE + " / " + OP_ADD_MULTIPLIED_BASE + " / " + OP_ADD_MULTIPLIED_TOTAL + "。",
        "修改 {attribute}：{gain}（{operation}）",
        List.of(
            ParamSpec.string("attribute", ""),
            ParamSpec.string("operation", OP_ADD_VALUE),
            ParamSpec.floating("gain", 1.0D),
            ParamSpec.floating("cost", 1.0D)),
        AttributeCommand::fromParams);

    private final Holder<Attribute> attribute;
    private final double gain;
    private final AttributeModifier.Operation operation;
    private final Identifier modifierId;

    /**
     * @param attribute  被修饰的属性
     * @param gain       属性数值（含义由 {@code operation} 决定）
     * @param operation  修饰方式
     * @param cost       消耗的点数
     * @param modifierId 修饰符 id，撤销时的唯一凭证
     */
    public AttributeCommand(
        Holder<Attribute> attribute,
        double gain,
        AttributeModifier.Operation operation,
        double cost,
        Identifier modifierId) {
        super(cost);
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.gain = gain;
        this.operation = Objects.requireNonNull(operation, "operation");
        this.modifierId = Objects.requireNonNull(modifierId, "modifierId");
    }

    private static AttributeModifier.Operation parseOperation(String text) {
        return switch (text) {
            case OP_ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case OP_ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case OP_ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> throw new IllegalArgumentException(
                "unknown attribute operation '" + text + "', expected one of "
                    + OP_ADD_VALUE + ", " + OP_ADD_MULTIPLIED_BASE + ", " + OP_ADD_MULTIPLIED_TOTAL);
        };
    }

    private static String operationName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> OP_ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> OP_ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> OP_ADD_MULTIPLIED_TOTAL;
        };
    }

    private static AttributeCommand fromParams(ParamValues values, CommandContext context) {
        Identifier attributeId = Identifier.tryParse(values.getString("attribute"));
        if (attributeId == null) {
            throw new IllegalArgumentException("invalid attribute id '" + values.getString("attribute") + "'");
        }
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId).orElse(null);
        if (attribute == null) {
            throw new IllegalArgumentException("unknown attribute '" + attributeId + "'");
        }
        double gain = values.getFloat("gain");
        if (Double.isNaN(gain) || Double.isInfinite(gain)) {
            throw new IllegalArgumentException("gain must be finite, but was " + gain);
        }
        AttributeModifier.Operation operation = parseOperation(values.getString("operation"));
        return new AttributeCommand(
            attribute, gain, operation, values.getFloat("cost"), context.modifierId());
    }

    public Holder<Attribute> attribute() {
        return this.attribute;
    }

    public double gain() {
        return this.gain;
    }

    public AttributeModifier.Operation operation() {
        return this.operation;
    }

    public Identifier modifierId() {
        return this.modifierId;
    }

    @Override
    public Identifier type() {
        return TYPE_ID;
    }

    @Override
    public MapCodec<AttributeCommand> codec() {
        return CODEC;
    }

    @Override
    public boolean doCommand(ServerPlayer player) {
        AttributeInstance target = player.getAttribute(this.attribute);
        if (target == null) {
            return false;
        }
        target.addOrReplacePermanentModifier(
            new AttributeModifier(this.modifierId, this.gain, this.operation));
        return true;
    }

    @Override
    public void undo(ServerPlayer player) {
        AttributeInstance target = player.getAttribute(this.attribute);
        if (target != null) {
            target.removeModifier(this.modifierId);
        }
    }
}
