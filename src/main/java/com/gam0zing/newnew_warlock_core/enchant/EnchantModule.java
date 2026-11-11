package com.gam0zing.newnew_warlock_core.enchant;

import com.gam0zing.newnew_warlock_core.core.ModModule;
import com.gam0zing.newnew_warlock_core.enchant.command.EnchantBaselineCommand;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;
import com.gam0zing.newnew_warlock_core.enchant.net.EnchantBaselineSync;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * 附魔模块：全局基准表加载、服务端快照同步与命令。
 */
public final class EnchantModule implements ModModule {

    @Override
    public String id() {
        return "enchant";
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        // common 配置在这里加载；附魔条目在注册表可用后补齐
        EnchantBaselineConfig.init();
        EnchantBaselineLoader.register();

        // 服务端基准快照同步（方案 B）与 /newnew enchant baseline 命令
        EnchantBaselineSync.register(modEventBus);
        EnchantBaselineCommand.register();
    }
}
