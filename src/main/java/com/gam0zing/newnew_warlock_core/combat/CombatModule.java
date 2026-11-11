package com.gam0zing.newnew_warlock_core.combat;

import com.gam0zing.newnew_warlock_core.combat.api.DamageHandlerRegistry;
import com.gam0zing.newnew_warlock_core.combat.api.DamageTags;
import com.gam0zing.newnew_warlock_core.combat.standard.InnerDamageHandler;
import com.gam0zing.newnew_warlock_core.combat.standard.VanillaDamageHandler;
import com.gam0zing.newnew_warlock_core.core.ModModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * 战斗模块：伤害标签 → 处理器的分发表。
 * <p>必须晚于核心模块，因为分发依赖标签与属性的注册结果；固化后运行期不再变化。</p>
 */
public final class CombatModule implements ModModule {

    @Override
    public String id() {
        return "combat";
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        registerDamageHandlers();
    }

    /**
     * 伤害处理器：按标签优先级从高到低扫描。
     * <p>内部伤害在插槽 0 短路；物理伤害的专用实现尚未完成，暂时指向原版处理器；
     * 普通伤害与兜底均走原版。</p>
     */
    private static void registerDamageHandlers() {
        VanillaDamageHandler vanilla = new VanillaDamageHandler();
        DamageHandlerRegistry.register(DamageTags.INNER_DAMAGE, 1000, new InnerDamageHandler());
        DamageHandlerRegistry.register(DamageTags.PHYSICS, 100, vanilla);
        DamageHandlerRegistry.register(DamageTags.NORMAL, 0, vanilla);
        DamageHandlerRegistry.registerFallback(0, vanilla);
        DamageHandlerRegistry.freeze();
    }
}
