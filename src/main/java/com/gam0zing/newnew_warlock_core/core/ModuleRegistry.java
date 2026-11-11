package com.gam0zing.newnew_warlock_core.core;

import java.util.Comparator;
import java.util.ServiceLoader;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * 模块注册表。
 * <p>模块清单来自 {@code META-INF/services/com.gam0zing.newnew_warlock_core.core.ModModule}，
 * 每个实现类一行；没有注解扫描、没有反射路径约定，新增模块只需要放入实现类并补一行清单。</p>
 * <p>按 {@link ModModule#priority()} 升序初始化（同优先级按 {@link ModModule#id()} 字典序），
 * 初始化只执行一次。</p>
 */
public final class ModuleRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean initialized;

    /**
     * 加载并初始化所有模块。
     *
     * @throws IllegalStateException 重复调用，或清单中的实现类无法实例化
     */
    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        if (initialized) {
            throw new IllegalStateException("模块注册表已初始化，不能重复调用");
        }
        initialized = true;

        ServiceLoader.load(ModModule.class, ModModule.class.getClassLoader()).stream()
            .map(ServiceLoader.Provider::get)
            .sorted(Comparator.comparingInt(ModModule::priority).thenComparing(ModModule::id))
            .forEach(module -> {
                LOGGER.info("初始化模块：{}（优先级 {}）", module.id(), module.priority());
                module.init(modEventBus, modContainer);
            });
    }

    private ModuleRegistry() {}
}
