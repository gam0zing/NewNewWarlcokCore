package com.gam0zing.newnew_warlock_core.share;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

/**
 * 威慑：被猫威慑的生物无法锁定目标，并持续远离威慑来源。
 * <p>伤害来源与持续时间由 {@code IntimidatePower} 施加时决定；受到非内部伤害会立刻移除本效果。</p>
 */
public final class IntimidatedMobEffect extends MobEffect {

    public IntimidatedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A5A3B);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        MobEffectInstance instance = entity.getEffect(ModMobEffects.INTIMIDATED);
        @Nullable LivingEntity source = EffectSources.sourceOf(instance);
        if (source == null || !source.isAlive() || source.level() != level
            || !(entity instanceof PathfinderMob mob)) {
            return true;
        }
        if (mob.position().distanceToSqr(source.position())
            > OriginTuning.CAT_INTIMIDATE_FLEE_RANGE * OriginTuning.CAT_INTIMIDATE_FLEE_RANGE) {
            // 已经跑出威慑范围，停止逃跑
            mob.getNavigation().stop();
            return true;
        }
        Vec3 away = mob.position().subtract(source.position());
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 target = mob.position().add(away.normalize().scale(OriginTuning.CAT_INTIMIDATE_FLEE_RANGE));
        mob.getNavigation().moveTo(target.x, target.y, target.z, 1.2D);
        return true;
    }
}
