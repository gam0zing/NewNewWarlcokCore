package com.gam0zing.newnew_warlock_core.effect;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 邪恶化属性的效果规则。
 * <p>大于 0 生效：受到的一切治疗减半（{@code Healing.calculate} 内处理）、免疫凋零；
 * 中毒仍可施加，但周期伤害被替换为同等级的生命恢复（{@code PoisonMobEffectMixin}）。</p>
 */
public final class Corruption {

    /**
     * {@return 这次效果施加是否被允许}
     * <p>邪恶化大于 0 的目标拒绝凋零效果的施加。</p>
     */
    public static boolean allowsEffect(LivingEntity target, MobEffectInstance instance) {
        if (ModAttributes.getValue(target, ModAttributes.CORRUPTION) <= 0.0D) {
            return true;
        }
        return !instance.is(MobEffects.WITHER);
    }

    private Corruption() {}
}
