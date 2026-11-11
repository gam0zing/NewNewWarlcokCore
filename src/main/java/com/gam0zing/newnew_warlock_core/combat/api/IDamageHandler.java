package com.gam0zing.newnew_warlock_core.combat.api;

import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * 单次伤害流程的全部业务阶段。
 * <p>伤害骨架本身不实现任何规则：它只负责容器数值同步、无敌帧状态存取、死亡分派与仇恨归因，
 * 每个阶段都原样转交给“本次伤害选中的处理器”。</p>
 * <p>处理器由 {@link DamageHandlerRegistry} 按伤害标签选出：伤害开始处理时顺序扫描注册表，
 * 第一个命中的标签对应的处理器接管本次流程。</p>
 * <p><b>全部方法都是抽象的。</b>没有默认实现、也没有基类：实现者必须逐项写出自己负责的行为，
 * 不需要的行为也要显式写成空方法或抛出异常，避免任何“看起来没做、其实走了默认逻辑”的黑箱。</p>
 */
public interface IDamageHandler {

    /** {@return 处理器的稳定标识} 用于注册表排序与诊断输出。 */
    Identifier id();

    /**
     * 插槽 0：内部伤害判定。
     * <p>返回 {@code true} 表示本次伤害由该处理器在本次调用内处理完毕，骨架立即返回
     * {@link DamageContext#isDealtDamage()}，不再进入后续任何阶段。</p>
     * <p>普通处理器必须返回 {@code false}；内部伤害处理器在这里完成直接扣血并返回 {@code true}。</p>
     */
    boolean isInner(DamageContext ctx);

    /**
     * 插槽 A：预处理。
     * <p>伤害进入流程后的最早时机，适合做分类修正、来源/目标标记、免疫判定、
     * 把事件注入的数值整理成业务结构等。</p>
     */
    void prepare(DamageContext ctx);

    /**
     * 插槽 B：盾牌行为。
     * <p>{@code LivingShieldBlockEvent} 已由骨架重发，格挡结果与格挡量已同步进上下文，
     * 这里负责格挡音效、粒子、缴械、反弹、完全格挡表现等；耐久见 {@link #onShieldDurability}。</p>
     */
    void onShieldBlock(DamageContext ctx);

    /**
     * 插槽 B2：盾牌耐久（防守方）。
     * <p>只在本次确实发生盾牌格挡时进入。</p>
     */
    void onShieldDurability(DamageContext ctx);

    /**
     * 插槽 C：主伤害计算。
     *
     * @param damage 进入主计算时的伤害（已完成预处理与盾牌格挡）
     * @return 自定义规则算出的伤害（之后仍会经过 {@code LivingDamageEvent.Pre}）
     */
    float calculate(DamageContext ctx, float damage);

    /**
     * 插槽 D：命中判定 / 无敌帧。
     * <p>在落地之前调用，决定本次伤害是否真正进入落地阶段。</p>
     *
     * @return {@code true} 继续落地；{@code false} 丢弃本次伤害（骨架直接返回 {@code false}）
     */
    boolean onGate(DamageContext ctx);

    /** 插槽 E：护甲耐久（防守方）。 */
    void onArmorDurability(DamageContext ctx);

    /**
     * 插槽 E2：{@code LivingDamageEvent.Pre} 之前的最后修正。
     * <p>处理器可以在这里改写容器上的伤害；Pre 事件与其他模组随后仍可继续修改，
     * 最终落地数值以 {@link #apply} 收到的参数为准。</p>
     */
    void onDamagePre(DamageContext ctx, DamageContainer container);

    /**
     * 插槽 F：落地。
     * <p>负责伤害吸收、扣血、战斗追踪、受击效果与击退等真正作用到目标身上的行为。</p>
     *
     * @param damage {@code LivingDamageEvent.Pre} 之后的伤害
     */
    void apply(DamageContext ctx, float damage);

    /** 插槽 G：受伤表现（受伤音效、受击动画、死亡音效等）。 */
    void playFeedback(DamageContext ctx);

    /**
     * 插槽 H：免死判定。
     *
     * @return {@code true} 表示本次已免死，骨架不再调用 {@code die}
     */
    boolean preventDeath(DamageContext ctx);

    /** 插槽 I：落地后的收尾（状态效果回调、统计、进度、食物消耗等）。 */
    void afterApplied(DamageContext ctx);

    /**
     * 插槽 F2：本次伤害产生的击退。
     * <p>由 {@link #apply} 在扣血之后调用；位移本身统一走 {@code LivingEntity#knockback}，
     * 因此 {@code LivingKnockBackEvent} 与其他模组的改写仍然生效。</p>
     *
     * @param dealtDamage 本次实际扣除的生命值
     */
    void applyKnockback(DamageContext ctx, float dealtDamage);
}
