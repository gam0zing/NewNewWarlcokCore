package com.gam0zing.newnew_warlock_core.effect;

import com.gam0zing.newnew_warlock_core.core.ModModule;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;
import com.gam0zing.newnew_warlock_core.share.MobEffectStackRegistry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * 效果模块：状态效果的叠加与合并策略。
 */
public final class EffectModule implements ModModule {

    @Override
    public String id() {
        return "effect";
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        // 出血按“多来源、多等级、多时长”方式合并，因此注册为容器管理效果
        MobEffectStackRegistry.register(ModMobEffects.BLEEDING, BleedingEffectStack::new);
        // 致命伤同样按多份施加累加等级
        MobEffectStackRegistry.register(ModMobEffects.FATAL_WOUND, FatalWoundEffectStack::new);
    }
}
