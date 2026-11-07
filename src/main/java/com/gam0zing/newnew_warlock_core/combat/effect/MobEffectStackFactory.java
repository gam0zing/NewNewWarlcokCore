package com.gam0zing.newnew_warlock_core.combat.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * 状态效果容器工厂。
 * <p>按需注册到 {@link MobEffectStackRegistry}：只有注册过的效果才会进入容器模式，
 * 其余效果保持原版行为。</p>
 * <p><b>约定</b>：工厂返回的容器必须是 {@link EffectStack} 的实现，
 * 且同时是某个具体子实例类型的子类（{@link EffectStack#self()} 会把它原样放进
 * {@code activeEffects}）。每个启用多实例的效果各自提供一个工厂，不做通用容器。</p>
 */
@FunctionalInterface
public interface MobEffectStackFactory {

    /**
     * 创建一个空容器。
     *
     * @param effect 容器对应的效果类型
     * @return 新容器，调用方随后通过 {@link EffectStack#pushChild} 填充子实例
     */
    EffectStack create(Holder<MobEffect> effect);
}
