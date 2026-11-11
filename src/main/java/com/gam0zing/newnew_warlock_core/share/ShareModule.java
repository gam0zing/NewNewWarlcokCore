package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 共享模块：属性、物品数据组件、状态效果的注册，以及它们的挂载/默认值事件。
 * <p>这些注册项被 core / combat / effect / mob / skill 等多个模块共同引用，
 * 因此放在独立的 {@code share} 空间而不是 {@code core}；优先级最高（0）。</p>
 */
public final class ShareModule implements ModModule {

    @Override
    public String id() {
        return "share";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        ModAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(AttributeEvt::onEntityAttributeModification);

        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        modEventBus.addListener(PhysicsDefaultComponents::onModifyDefaultComponents);

        ModMobEffects.MOB_EFFECTS.register(modEventBus);
    }
}
