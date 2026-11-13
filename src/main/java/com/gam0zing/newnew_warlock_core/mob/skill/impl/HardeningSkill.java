package com.gam0zing.newnew_warlock_core.mob.skill.impl;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.skill.CooldownMobSkill;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 硬化：价值 10，普通技能。
 * <p>每 3 秒给自己施加一次持续 3 秒的【伤害吸收】，效果等级等于技能等级。</p>
 */
public final class HardeningSkill extends CooldownMobSkill {

    private static final int INTERVAL_TICKS = 60;
    private static final int EFFECT_DURATION_TICKS = 60;

    public HardeningSkill() {
        super(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "hardening"), 10.0F, INTERVAL_TICKS);
    }

    @Override
    public boolean getTrigger(LivingEntity self, int level) {
        return true;
    }

    @Override
    public void invoke(LivingEntity self, int level) {
        self.addEffect(new MobEffectInstance(
            MobEffects.ABSORPTION, EFFECT_DURATION_TICKS, Math.max(0, level - 1)));
    }

    @Override
    public void onCooldown(LivingEntity self, int level) {
        // 冷却期间不需要额外逻辑
    }
}
