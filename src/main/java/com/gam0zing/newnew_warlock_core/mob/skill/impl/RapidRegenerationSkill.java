package com.gam0zing.newnew_warlock_core.mob.skill.impl;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.skill.CooldownMobSkill;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 急速再生：价值 10，普通技能。
 * <p>每 20 秒给自己施加持续 5 秒的【生命恢复】（等级 = 技能等级 × 2）和【抗性提升 III】。</p>
 */
public final class RapidRegenerationSkill extends CooldownMobSkill {

    private static final int INTERVAL_TICKS = 400;
    private static final int EFFECT_DURATION_TICKS = 100;
    private static final int RESISTANCE_AMPLIFIER = 2;

    public RapidRegenerationSkill() {
        super(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "rapid_regeneration"), 10.0F,
            INTERVAL_TICKS);
    }

    @Override
    public boolean getTrigger(LivingEntity self, int level) {
        return true;
    }

    @Override
    public void invoke(LivingEntity self, int level) {
        self.addEffect(new MobEffectInstance(
            MobEffects.REGENERATION, EFFECT_DURATION_TICKS, Math.max(0, level * 2 - 1)));
        self.addEffect(new MobEffectInstance(
            MobEffects.RESISTANCE, EFFECT_DURATION_TICKS, RESISTANCE_AMPLIFIER));
    }

    @Override
    public void onCooldown(LivingEntity self, int level) {
        // 冷却期间不需要额外逻辑
    }
}
