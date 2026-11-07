package com.gam0zing.newnew_warlock_core.combat.damage;

/**
 * 玩家攻击时原版会发出的粒子类型。
 * <p>用于 {@link DamagePipeline#handleAttackParticle} 区分业务上允许哪一种粒子。</p>
 */
public enum AttackParticleType {
    /** 暴击粒子（{@code ServerPlayer#crit} → {@code ClientboundAnimatePacket} 4） */
    CRIT,
    /** 附魔额外伤害粒子（{@code ServerPlayer#magicCrit} → {@code ClientboundAnimatePacket} 5） */
    MAGIC_CRIT,
    /** 横扫粒子（{@code Player#sweepAttack} → {@code ParticleTypes.SWEEP_ATTACK}） */
    SWEEP,
    /** 受击指示粒子（{@code Player#attack} → {@code ParticleTypes.DAMAGE_INDICATOR}） */
    DAMAGE_INDICATOR
}
