package com.gam0zing.newnew_warlock_core;

import org.slf4j.Logger;

import com.gam0zing.newnew_warlock_core.core.ModuleRegistry;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * 模组入口。
 * <p>这里只保留模组标识与日志器；全部注册内容按模块拆分，由
 * {@link ModuleRegistry} 从 {@code META-INF/services} 清单加载并初始化，
 * 本类不再直接引用任何注册表或业务类。</p>
 */
@Mod(NewNewWarlockCore.MOD_ID)
public class NewNewWarlockCore {
    public static final String MOD_ID = "newnew_warlock_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NewNewWarlockCore(IEventBus modEventBus, ModContainer modContainer) {
        ModuleRegistry.init(modEventBus, modContainer);
    }
}
