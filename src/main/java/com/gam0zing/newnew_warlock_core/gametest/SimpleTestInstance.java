package com.gam0zing.newnew_warlock_core.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 以代码方式注册的 GameTest 实例。
 *
 * <p>26.3 移除了 {@code @GameTest} 注解，测试必须作为 {@link GameTestInstance} 注册到
 * {@code RegisterGameTestsEvent}。这里用一个极简包装把「{@code Consumer<GameTestHelper>}」变成实例。</p>
 *
 * <p>{@link #codec()} 必须对应一个注册在 {@code minecraft:test_instance_type} 里的类型，
 * 否则进入存档同步 {@code minecraft:test_instance} 注册表时会因查不到类型键而报
 * “Unregistered holder”。占位 codec 本身不承载数据，只需能被反查到即可。</p>
 */
public final class SimpleTestInstance extends GameTestInstance {

    private static final TestData<Holder<TestEnvironmentDefinition<?>>> EMPTY_INFO = new TestData<>(
        null, Identifier.parse("minecraft:empty"), 100, 0, true);

    /** 用 supplier 而不是单例：注册表同步会逐个解码，共享同一实例会被判为重复值。 */
    private static final MapCodec<SimpleTestInstance> CODEC =
        MapCodec.unit(() -> new SimpleTestInstance(EMPTY_INFO, helper -> {}));

    /** 把占位 codec 注册为 {@code newnew_warlock_core:function} 测试实例类型，供注册表同步时反查。 */
    public static void registerType(RegisterEvent event) {
        event.register(
            Registries.TEST_INSTANCE_TYPE,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "function"),
            () -> CODEC);
    }

    private final Consumer<GameTestHelper> function;

    public SimpleTestInstance(
        TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> function) {
        super(info);
        this.function = function;
    }

    @Override
    public void run(GameTestHelper helper) {
        this.function.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("newnew_warlock_core:function");
    }
}
