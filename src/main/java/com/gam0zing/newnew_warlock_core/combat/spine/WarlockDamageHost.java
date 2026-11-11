package com.gam0zing.newnew_warlock_core.combat.spine;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * 由 mixin 添加到 {@link LivingEntity} 上的管线宿主接口，用于让事件处理器与业务插槽能够调用
 * 重写后的完整伤害流程（该流程需要访问 LivingEntity 的 protected/private 成员）。
 * <p>下面的装备耐久方法都是“原版等价入口”，供业务插槽按需调用：</p>
 * <ul>
 *     <li>它们会照常发出外部事件（{@code ArmorHurtEvent}），保证与其他模组的兼容；</li>
 *     <li>是否调用、何时调用、传入多少，全部由业务插槽决定。</li>
 * </ul>
 */
public interface WarlockDamageHost {
    /**
     * 执行重写后的伤害管线。须在 {@code LivingIncomingDamageEvent} 被取消之后调用，
     * 此时原版 {@code LivingEntity#hurt} 已经停止执行。
     *
     * @param container 本次伤害流程的容器（仍在 {@code LivingEntity#damageContainers} 栈顶）
     * @return 原版 {@code hurt} 本应返回的结果（是否真正产生了伤害）
     */
    boolean newnew$runDamagePipeline(DamageContainer container);

    /**
     * 原版护甲耐久入口（四个护甲槽位，对任意生物都有效）。
     * <p>内部会发出 {@code ArmorHurtEvent}（可取消、可改数值），再按事件数值扣耐久。</p>
     *
     * @param damage 传入原版的伤害值：原版规则为“每 4 点伤害 1 点耐久，至少 1”
     */
    void newnew$hurtArmorItems(DamageSource source, float damage);

    /**
     * 原版头盔耐久入口（只作用于头盔槽，对应原版 {@code DAMAGES_HELMET} 的处理）。
     *
     * @param damage 传入原版的伤害值（规则同上）
     */
    void newnew$hurtHelmetItem(DamageSource source, float damage);

    /**
     * 原版盾牌耐久入口（对应原版 {@code hurtCurrentlyUsedShield}）。
     * <p>玩家会走完整原版逻辑：统计、耐久扣除、损坏事件、停止使用物品等；
     * 非玩家生物在原版里没有盾牌使用逻辑，因此为无操作。</p>
     *
     * @param blockedDamage 本次盾牌格挡掉的伤害
     */
    void newnew$hurtShield(float blockedDamage);

    // ----------------------------------------------------------------------------------
    // 无敌帧状态：骨架只负责存取，判定规则在处理器里（原版规则见 VanillaDamageUtils#onGate）。
    // ----------------------------------------------------------------------------------

    /** {@return 最近一次生效命中所在游戏刻} 从未命中时为 {@code -1000}。 */
    long newnew$getLastHitGameTime();

    void newnew$setLastHitGameTime(long gameTime);

    /** {@return 最近一次生效命中的伤害值} 用于无敌帧内的伤害比较与减免。 */
    float newnew$getLastHurt();

    void newnew$setLastHurt(float damage);

    // ----------------------------------------------------------------------------------
    // 临时：恢复原版受伤表现（受伤音效、实体红闪、死亡音效）。这几个原版方法在 LivingEntity 里是
    // protected/private，业务区无法直接调用，因此通过宿主接口暴露；表现逻辑在
    // VanillaCombatViewHelper#playHurtFeedback。重新屏蔽表现时，这些方法可以一并删除。
    // ----------------------------------------------------------------------------------

    /** 原版受伤状态标记（{@code LivingEntity#markHurt}：hurtDuration = hurtTime = 10）。 */
    void newnew$markHurt();

    /** 原版受伤音效（{@code LivingEntity#playHurtSound}）。 */
    void newnew$playHurtSound(DamageSource source);

    /** 原版死亡音效（{@code makeSound(getDeathSound())}）。 */
    void newnew$playDeathSound();

    /**
     * 原版护甲吸收（{@code LivingEntity#getDamageAfterArmorAbsorb}）。
     * <p>内部同时处理 {@code BYPASSES_ARMOR} 与护甲耐久消耗（{@code hurtArmor}、{@code ArmorHurtEvent}）。</p>
     */
    float newnew$getDamageAfterArmorAbsorb(DamageSource source, float damage);

    /**
     * 原版保护附魔 + 抗性提升吸收（{@code LivingEntity#getDamageAfterMagicAbsorb}）。
     * <p>内部处理 {@code BYPASSES_EFFECTS} / {@code BYPASSES_RESISTANCE}。</p>
     */
    float newnew$getDamageAfterMagicAbsorb(DamageSource source, float damage);
}
