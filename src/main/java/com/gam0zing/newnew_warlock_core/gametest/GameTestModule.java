package com.gam0zing.newnew_warlock_core.gametest;

import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * GameTest 模块：测试实例与测试用例的注册。
 * <p>只影响开发期测试入口，运行期没有业务逻辑。</p>
 */
public final class GameTestModule implements ModModule {

    @Override
    public String id() {
        return "gametest";
    }

    @Override
    public int priority() {
        return 90;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModGameTests::register);
        modEventBus.addListener(SimpleTestInstance::registerType);
    }
}
