package com.gam0zing.newnew_warlock_core.combat.spine;

import net.minecraft.world.entity.LivingEntity;

/**
 * 由 mixin 织入 {@link LivingEntity} 的“带来源治疗”接口。
 * <p>调用方式：{@code ((WarlockHealingHost) livingEntity).healByLiving(source, amount)}。</p>
 */
public interface WarlockHealingHost {

    /**
     * 带来源生物的治疗。
     * <p>内部先做治疗量计算（含治疗强度等），再调用生物自身的 {@link LivingEntity#heal(float)}
     * （因此 {@code LivingHealEvent} 等外部钩子照常触发）。</p>
     *
     * @param source 治疗来源生物，可为 null（等价于没有治疗强度加成）
     * @param amount 基础治疗量
     * @return 实际恢复的血量（目标已满血/已死亡或治疗量为 0 时返回 0）
     */
    float healByLiving(LivingEntity source, float amount);
}
