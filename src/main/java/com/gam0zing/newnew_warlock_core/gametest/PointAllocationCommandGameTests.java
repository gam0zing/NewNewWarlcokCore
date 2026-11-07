package com.gam0zing.newnew_warlock_core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.registry.ModAttributes;
import com.gam0zing.newnew_warlock_core.skill.command.AttributeCommand;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommand;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommands;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * 点数分配命令的验证：注册表分发、NBT 往返、属性修饰的施加/撤销、点数计算。
 * <p>全部不依赖技能树结构，命令单独可用。</p>
 */
public final class PointAllocationCommandGameTests {

    private static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "alloc/test");

    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        register(event, environment, "codec_round_trip", PointAllocationCommandGameTests::commandCodecRoundTrip);
        register(event, environment, "unknown_type_fails", PointAllocationCommandGameTests::unknownTypeFailsToParse);
        register(event, environment, "attribute_apply_undo",
            PointAllocationCommandGameTests::attributeCommandAppliesAndUndoes);
        register(event, environment, "attribute_multiply",
            PointAllocationCommandGameTests::attributeCommandSupportsMultiplyOperations);
    }

    private static void register(
        RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
        String name, java.util.function.Consumer<GameTestHelper> function) {
        event.registerTest(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "point_allocation/" + name),
            new SimpleTestInstance(ModGameTests.info(environment, ModGameTests.emptyStructure(), 100), function));
    }

    /** 命令编码后带注册键，解码能回到同类型同数值的实例。 */
    public static void commandCodecRoundTrip(GameTestHelper helper) {
        PointAllocationCommand original = new AttributeCommand(ModAttributes.NEWNEW_STRENGTH, 2.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 3.0D, MODIFIER_ID);

        CompoundTag tag = (CompoundTag) PointAllocationCommands.CODEC
            .encodeStart(NbtOps.INSTANCE, original)
            .getOrThrow();
        String encodedType = tag.getStringOr("type", "");
        helper.assertTrue(AttributeCommand.TYPE_ID.toString().equals(encodedType),
            "编码结果缺少正确的 type，实际为 " + encodedType);

        PointAllocationCommand decoded = PointAllocationCommands.CODEC
            .parse(NbtOps.INSTANCE, tag)
            .getOrThrow();
        helper.assertTrue(decoded instanceof AttributeCommand,
            "解码结果类型不是 AttributeCommand，实际为 " + decoded.getClass().getName());

        AttributeCommand command = (AttributeCommand) decoded;
        helper.assertTrue(command.attribute().equals(ModAttributes.NEWNEW_STRENGTH),
            "解码后属性不一致：" + command.attribute());
        helper.assertTrue(command.gain() == 2.0D, "解码后增幅不一致：" + command.gain());
        helper.assertTrue(command.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            "解码后修饰方式不一致：" + command.operation());
        helper.assertTrue(command.cost() == 3.0D, "解码后消耗不一致：" + command.cost());
        helper.assertTrue(command.modifierId().equals(MODIFIER_ID),
            "解码后修饰符 id 不一致：" + command.modifierId());

        helper.succeed();
    }

    /** 未知类型必须解析失败，而不是静默跳过。 */
    public static void unknownTypeFailsToParse(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "newnew_warlock_core:not_registered");
        helper.assertTrue(PointAllocationCommands.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent(),
            "未知命令类型却解析成功了");

        CompoundTag noType = new CompoundTag();
        helper.assertTrue(PointAllocationCommands.CODEC.parse(NbtOps.INSTANCE, noType).error().isPresent(),
            "缺少 type 字段却解析成功了");

        helper.succeed();
    }

    /** 执行给属性加修饰符、撤销移除；剩余点数按“上限 − 已分配”计算。 */
    public static void attributeCommandAppliesAndUndoes(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "newnew_test"));

        AttributeInstance points = player.getAttribute(ModAttributes.NEWNEW_POINTS);
        helper.assertTrue(points != null, "测试玩家没有 newnew_points 属性");
        points.setBaseValue(10.0D);

        AttributeInstance strength = player.getAttribute(ModAttributes.NEWNEW_STRENGTH);
        helper.assertTrue(strength != null, "测试玩家没有 newnew_strength 属性");
        double baseStrength = strength.getBaseValue();

        List<PointAllocationCommand> applied = new ArrayList<>();
        PointAllocationCommand command = new AttributeCommand(ModAttributes.NEWNEW_STRENGTH, 2.0D,
            AttributeModifier.Operation.ADD_VALUE, 3.0D, MODIFIER_ID);

        helper.assertTrue(PointAllocationCommand.remaining(player, applied) == 10.0D,
            "初始剩余点数应为 10，实际为 " + PointAllocationCommand.remaining(player, applied));

        helper.assertTrue(command.doCommand(player), "doCommand 执行失败");
        applied.add(command);

        helper.assertTrue(strength.getValue() == baseStrength + 2.0D,
            "执行后力量应为 " + (baseStrength + 2.0D) + "，实际为 " + strength.getValue());
        helper.assertTrue(PointAllocationCommand.remaining(player, applied) == 7.0D,
            "执行后剩余点数应为 7，实际为 " + PointAllocationCommand.remaining(player, applied));

        // 幂等：重复执行不应叠加
        helper.assertTrue(command.doCommand(player), "重复执行 doCommand 失败");
        helper.assertTrue(strength.getValue() == baseStrength + 2.0D,
            "重复执行后力量被叠加为 " + strength.getValue());

        command.undo(player);
        applied.remove(command);

        helper.assertTrue(strength.getValue() == baseStrength,
            "撤销后力量应回到 " + baseStrength + "，实际为 " + strength.getValue());
        helper.assertTrue(PointAllocationCommand.remaining(player, applied) == 10.0D,
            "撤销后剩余点数应为 10，实际为 " + PointAllocationCommand.remaining(player, applied));

        helper.succeed();
    }

    /** 修饰方式生效：乘算基值与乘算总值按各自公式作用，撤销后回到原值。 */
    public static void attributeCommandSupportsMultiplyOperations(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "newnew_test"));

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        helper.assertTrue(health != null, "测试玩家没有 max_health 属性");
        health.setBaseValue(20.0D);

        PointAllocationCommand multipliedBase = new AttributeCommand(Attributes.MAX_HEALTH, 0.5D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.0D,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "alloc/mult_base"));
        helper.assertTrue(multipliedBase.doCommand(player), "乘算基值执行失败");
        helper.assertTrue(Math.abs(health.getValue() - 30.0D) < 1.0E-6D,
            "乘算基值后最大生命应为 30，实际为 " + health.getValue());
        multipliedBase.undo(player);
        helper.assertTrue(Math.abs(health.getValue() - 20.0D) < 1.0E-6D,
            "撤销乘算基值后应回到 20，实际为 " + health.getValue());

        PointAllocationCommand multipliedTotal = new AttributeCommand(Attributes.MAX_HEALTH, 0.5D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 1.0D,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "alloc/mult_total"));
        helper.assertTrue(multipliedTotal.doCommand(player), "乘算总值执行失败");
        helper.assertTrue(Math.abs(health.getValue() - 30.0D) < 1.0E-6D,
            "乘算总值后最大生命应为 30，实际为 " + health.getValue());
        multipliedTotal.undo(player);
        helper.assertTrue(Math.abs(health.getValue() - 20.0D) < 1.0E-6D,
            "撤销乘算总值后应回到 20，实际为 " + health.getValue());

        helper.succeed();
    }

    private PointAllocationCommandGameTests() {}
}
