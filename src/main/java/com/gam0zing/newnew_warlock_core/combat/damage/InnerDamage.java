package com.gam0zing.newnew_warlock_core.combat.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 内部伤害：属于 {@code inner_damage} 标签的伤害不进入完整伤害管线。
 * <p>由伤害管线在入口按标签显式路由到这里，不经过护甲、附魔、药水状态、伤害强度加成、
 * 无敌帧与 {@code LivingDamageEvent}，按给定数值直接扣除生命值。</p>
 * <p>仍保留伤害来源，用于战斗记录与死亡信息；生命值归零时走正常死亡流程（{@link LivingEntity#die}）。</p>
 * <p>原版有少数伤害是在每刻循环里裸调 {@code hurt} 的（怕水生物与蜜蜂的溺水、海豚的脱水），
 * 它们靠无敌帧降频；这些调用点已各自加入间隔，见 {@link PeriodicDamageGate}。</p>
 */
public final class InnerDamage {

    /**
     * 直接扣除目标生命值。
     *
     * @param target 目标；客户端或已处于死亡状态时不做任何事
     * @param source 伤害来源（其伤害类型应当位于 {@code inner_damage} 标签）
     * @param amount 伤害量；非正数、非有限值或目标没有生命值时不做任何事
     * @return 是否实际扣除了生命值
     */
    public static boolean apply(LivingEntity target, DamageSource source, float amount) {
        if (target.level().isClientSide() || target.isDeadOrDying()) {
            return false;
        }

        float current = target.getHealth();
        if (!(current > 0.0F) || !(amount > 0.0F) || !Float.isFinite(amount)) {
            return false;
        }

        float actual = Math.min(amount, current);
        target.setHealth(current - actual);
        target.getCombatTracker().recordDamage(source, actual);

        if (target.isDeadOrDying()) {
            target.die(source);
        }
        return true;
    }

    private InnerDamage() {}
}
