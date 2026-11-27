package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/**
 * 冰冻：寒冷群系中史莱姆的状态。
 * <p>削减 = -20% + 30% × 等级（I 级 10%、II 级 40%、III 级 70%、IV 级 100%），
 * 同时按相同数值提高受伤倍率；削减达到 100% 后每 5 秒受到 2 × 等级的内部伤害。</p>
 */
public final class SlimeFrozenMobEffect extends SlimeClimateMobEffect {

    private static final ResourceKey<DamageType> DAMAGE_TYPE = damageTypeKey("frozen");

    public SlimeFrozenMobEffect() {
        super(0x7FD4FF);
    }

    /** {@return 该等级的速度/跳跃削减与受伤加成比例} */
    public static float reductionFor(int level) {
        float value = OriginTuning.SLIME_FROZEN_REDUCTION_BASE
            + OriginTuning.SLIME_FROZEN_REDUCTION_PER_LEVEL * level;
        return Math.max(0.0F, value);
    }

    @Override
    protected Holder<MobEffect> self() {
        return ModMobEffects.SLIME_FROZEN;
    }

    @Override
    protected BiomeClimate matchingClimate() {
        return BiomeClimate.COLD;
    }

    @Override
    public float reduction(int level) {
        return reductionFor(level);
    }

    @Override
    protected float periodicDamage(int level) {
        return reductionFor(level) >= 1.0F
            ? OriginTuning.SLIME_CLIMATE_DAMAGE_PER_LEVEL * level
            : 0.0F;
    }

    @Override
    protected ResourceKey<DamageType> damageType() {
        return DAMAGE_TYPE;
    }
}
