package com.gam0zing.newnew_warlock_core.combat.spine;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.world.entity.LivingEntity;

/**
 * 治疗计算（业务区域）。
 * <p>所有“治疗量该怎么算”的规则集中在这里；织入的 {@code healByLiving} 只负责调用计算并落地。</p>
 */
public final class Healing {

    /**
     * 计算最终治疗量。
     *
     * @param target 被治疗者
     * @param source 治疗来源生物，可为 null
     * @param amount 基础治疗量
     * @return 经过计算的最终治疗量（不会小于 0）
     */
    public static float calculate(LivingEntity target, @Nullable LivingEntity source, float amount) {
        if (!(amount > 0.0F) || !Float.isFinite(amount)) {
            return 0.0F;
        }

        // 治疗强度：由治疗来源生物提供，缺失该属性的实体按基础值 1.0 处理
        amount *= getHealingScale(source);

        // 受到治疗倍率：由被治疗者自己持有
        amount *= ModAttributes.getScale(target, ModAttributes.RECEIVE_HEALING_SCALE);

        // 邪恶化：受到的一切治疗效果减半
        if (ModAttributes.getValue(target, ModAttributes.CORRUPTION) > 0.0D) {
            amount *= 0.5F;
        }

        return Math.max(0.0F, amount);
    }

    /** {@return 来源生物的治疗强度；没有该属性（如自定义实体）时按基础值 1.0} */
    public static float getHealingScale(@Nullable LivingEntity source) {
        return ModAttributes.getScale(source, ModAttributes.HEALING_SCALE);
    }

    private Healing() {}
}
