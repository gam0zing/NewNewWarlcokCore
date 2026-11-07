package com.gam0zing.newnew_warlock_core.gametest;

import java.nio.file.Path;

import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * GameTest 注册入口。
 *
 * <p>26.3 起测试不再由注解自动发现，必须在 {@link RegisterGameTestsEvent} 中显式登记。</p>
 */
public final class ModGameTests {

    private ModGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        // 26.3 已不再自动指向开发目录，且结构加载改为数据包布局（data/<ns>/structure/*.snbt）。
        // 这里在服务器创建前把测试结构源目录指到 run/gameteststructures。
        StructureUtils.testStructuresSourceDir = Path.of("gameteststructures");

        var environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "default"));

        BodyEffectDataGameTests.register(event, environment);
        EnchantBaselineGameTests.register(event, environment);
        PointAllocationCommandGameTests.register(event, environment);
    }

    /** 测试结构统一使用模组自带的空结构 */
    public static Identifier emptyStructure() {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "empty");
    }

    public static Identifier enchantTableStructure() {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "enchant_table");
    }

    static net.minecraft.gametest.framework.TestData<net.minecraft.core.Holder<TestEnvironmentDefinition<?>>> info(
        net.minecraft.core.Holder<TestEnvironmentDefinition<?>> environment, Identifier structure, int maxTicks) {
        return new net.minecraft.gametest.framework.TestData<>(environment, structure, maxTicks, 0, true);
    }
}
