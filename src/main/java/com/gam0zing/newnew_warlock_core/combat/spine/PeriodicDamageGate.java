package com.gam0zing.newnew_warlock_core.combat.spine;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * 原版“每刻调用 {@code hurt}、靠无敌帧降频”的伤害来源的节奏控制。
 * <p>原版有几处伤害是在每刻循环里裸调 {@code hurt} 的：怕水生物在雨中/水中的溺水、
 * 蜜蜂水下溺水、海豚脱水。它们本身没有计时器，实际间隔完全由无敌帧
 * （{@code invulnerableTime} 置 20，{@code > 10} 时挡下，即每 10 刻一次）提供。</p>
 * <p>内部伤害不经过无敌帧，因此这些调用点必须自己限速，否则会每刻结算一次。
 * 这里按“目标 + 伤害类型”记录上次结算时刻，间隔与原版无敌帧形成的节奏一致。</p>
 */
public interface PeriodicDamageGate {

    /** 原版无敌帧形成的实际结算间隔（刻） */
    int VANILLA_INTERVAL_TICKS = 10;

    /**
     * 判断本次是否允许结算。
     *
     * @param source 伤害来源；只对溺水与脱水的伤害类型限速，其余类型恒为 true
     * @return 是否允许结算；允许时会记录当前游戏刻
     */
    boolean newnew$allowPeriodicDamage(DamageSource source);

    /**
     * 限速入口，供各调用点的注入使用。
     *
     * @param entity 承受伤害的生物
     * @param source 伤害来源
     * @return 是否允许本次结算
     */
    static boolean allow(LivingEntity entity, DamageSource source) {
        if (!source.is(DamageTypes.DROWN) && !source.is(DamageTypes.DRY_OUT)) {
            return true;
        }
        return !(entity instanceof PeriodicDamageGate gate) || gate.newnew$allowPeriodicDamage(source);
    }
}
