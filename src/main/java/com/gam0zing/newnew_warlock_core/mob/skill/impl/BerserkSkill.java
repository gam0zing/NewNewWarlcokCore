package com.gam0zing.newnew_warlock_core.mob.skill.impl;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.skill.CooldownMobSkill;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 狂暴：价值 5，冷却 10 秒。
 * <p>触发条件：当前存在仇恨目标。触发后给自己施加 3 秒的【迅捷】（等级 = 技能等级）
 * 和【不可阻挡 III】。</p>
 */
public final class BerserkSkill extends CooldownMobSkill {

    private static final int COOLDOWN_TICKS = 200;
    private static final int EFFECT_DURATION_TICKS = 60;
    private static final int UNSTOPPABLE_AMPLIFIER = 2;

    public BerserkSkill() {
        super(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "berserk"), 5.0F, COOLDOWN_TICKS);
    }

    @Override
    public boolean getTrigger(LivingEntity self, int level) {
        if (!(self instanceof Mob mob)) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void invoke(LivingEntity self, int level) {
        self.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION_TICKS, Math.max(0, level - 1)));
        self.addEffect(new MobEffectInstance(
            ModMobEffects.UNSTOPPABLE, EFFECT_DURATION_TICKS, UNSTOPPABLE_AMPLIFIER));
    }

    @Override
    public void onCooldown(LivingEntity self, int level) {
        // 冷却期间不需要额外逻辑
    }
}
