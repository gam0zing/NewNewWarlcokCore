package com.gam0zing.newnew_warlock_core.mob.skill.impl;

import java.util.List;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.skill.CooldownMobSkill;
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 衰竭：价值 20，普通技能。
 * <p>每 5 秒对自身周围 5×5 格内的所有生物（不含自己）随机施加【大出血】或【致命伤】，
 * 两者概率相等，效果等级等于技能等级。新增效果时直接往 {@link #EFFECTS} 里加一项即可。</p>
 */
public final class DecaySkill extends CooldownMobSkill {

    private static final int INTERVAL_TICKS = 100;
    private static final double AREA_RADIUS = 2.5D;
    private static final int FATAL_WOUND_DURATION_TICKS = 200;

    /** 可随机施加的效果列表，按索引等概率抽取 */
    private static final List<EffectChoice> EFFECTS = List.of(
        new EffectChoice(ModMobEffects.HEMORRHAGE, 1),
        new EffectChoice(ModMobEffects.FATAL_WOUND, FATAL_WOUND_DURATION_TICKS));

    private record EffectChoice(DeferredHolder<MobEffect, MobEffect> effect, int duration) {}

    public DecaySkill() {
        super(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "decay"), 20.0F, INTERVAL_TICKS);
    }

    @Override
    public boolean getTrigger(LivingEntity self, int level) {
        return true;
    }

    @Override
    public void invoke(LivingEntity self, int level) {
        AABB area = self.getBoundingBox().inflate(AREA_RADIUS);
        for (LivingEntity target : self.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == self || !target.isAlive()) {
                continue;
            }
            EffectChoice choice = EFFECTS.get(self.getRandom().nextInt(EFFECTS.size()));
            EffectSources.apply(
                target,
                new MobEffectInstance(choice.effect(), choice.duration(), Math.max(0, level - 1)),
                self);
        }
    }

    @Override
    public void onCooldown(LivingEntity self, int level) {
        // 冷却期间不需要额外逻辑
    }
}
