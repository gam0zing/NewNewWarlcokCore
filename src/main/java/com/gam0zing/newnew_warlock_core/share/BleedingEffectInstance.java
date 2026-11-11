package com.gam0zing.newnew_warlock_core.share;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 出血的子实例类型。
 *
 * <p>{@code activeEffects} 里保存的干燥效果对象、{@code getEffect(BLEEDING)} 的返回值、
 * 迭代拿到的对象都是本类型（容器 {@link BleedingEffectStack} 直接继承本类），
 * 因此业务侧可以安全向下转型并调用出血专属的实例级 API。</p>
 *
 * <p>出血专属的实例状态与 API 都应放在本类；容器按需重写同名方法把它们聚合起来。</p>
 */
public class BleedingEffectInstance extends MobEffectInstance {

    public BleedingEffectInstance(Holder<MobEffect> effect) {
        super(effect);
    }

    public BleedingEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) {
        super(effect, duration, amplifier);
    }

    /**
     * 由普通实例升级为本类型实例。
     * <p>复制原版的时长、等级、可见性与来源 UUID 等全部字段；
     * 原版复制构造不携带 {@code hiddenEffect}，容器模型同样不使用隐藏效果。</p>
     */
    public BleedingEffectInstance(MobEffectInstance other) {
        super(other);
    }
}
