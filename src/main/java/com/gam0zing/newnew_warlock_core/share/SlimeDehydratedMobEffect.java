package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;

/**
 * 失水：炎热干燥群系中史莱姆的状态。
 * <p>每级削减 10% 的总移动速度与跳跃初速；1 级不造成伤害，2 级起每 5 秒受到 2 ×（等级-1）的内部伤害。</p>
 */
public final class SlimeDehydratedMobEffect extends SlimeClimateMobEffect {

    private static final ResourceKey<DamageType> DAMAGE_TYPE = damageTypeKey("dehydrated");

    public SlimeDehydratedMobEffect() {
        super(0xE0A23C);
    }

    /** {@return 该等级的总移动速度/跳跃初速削减比例} */
    public static float reductionFor(int level) {
        return Math.max(0.0F, OriginTuning.SLIME_DEHYDRATED_REDUCTION_PER_LEVEL * level);
    }

    @Override
    protected Holder<MobEffect> self() {
        return ModMobEffects.SLIME_DEHYDRATED;
    }

    @Override
    protected BiomeClimate matchingClimate() {
        return BiomeClimate.HOT_DRY;
    }

    @Override
    public float reduction(int level) {
        return reductionFor(level);
    }

    @Override
    protected float periodicDamage(int level) {
        return OriginTuning.SLIME_CLIMATE_DAMAGE_PER_LEVEL * (level - 1);
    }

    @Override
    protected ResourceKey<DamageType> damageType() {
        return DAMAGE_TYPE;
    }
}
