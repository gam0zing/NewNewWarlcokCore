package com.gam0zing.newnew_warlock_core.combat.spine;

/**
 * 伤害骨架的插槽入口。
 * <p>链路本身（{@code LivingEntityDamagePipelineMixin}、{@code LivingEntityKnockbackGateMixin}）
 * 只保留骨架与外部注入钩子：容器数值同步、无敌帧状态存取、死亡分派、仇恨归因，以及
 * {@code LivingShieldBlockEvent}、{@code ArmorHurtEvent}、{@code LivingDamageEvent.Pre/Post}、
 * {@code LivingDeathEvent} 等事件的重发。</p>
 * <p><b>这里没有任何业务实现：</b>每个插槽都只把调用转给本次伤害选中的处理器
 * {@link com.gam0zing.newnew_warlock_core.combat.api.IDamageHandler}。
 * 具体规则在处理器实现类里，按伤害标签由
 * {@link com.gam0zing.newnew_warlock_core.combat.api.DamageHandlerRegistry} 选出。</p>
 */
public final class DamagePipeline {

    /**
     * 插槽 0：内部伤害（入口短路）。
     *
     * @return {@code true} 表示处理器已处理完毕，链路应直接返回
     */
    public static boolean routeInnerDamage(DamageContext ctx) {
        return ctx.getHandler().isInner(ctx);
    }

    /** 插槽 A：预处理。 */
    public static void prepare(DamageContext ctx) {
        ctx.getHandler().prepare(ctx);
    }

    /** 插槽 B：盾牌行为。 */
    public static void handleShieldBlock(DamageContext ctx) {
        ctx.getHandler().onShieldBlock(ctx);
    }

    /** 插槽 B2：盾牌耐久。 */
    public static void applyShieldDurability(DamageContext ctx) {
        ctx.getHandler().onShieldDurability(ctx);
    }

    /**
     * 插槽 C：主伤害计算。
     *
     * @param damage 进入主计算时的伤害（已完成预处理与盾牌格挡）
     * @return 处理器算出的伤害（之后仍会经过 {@code LivingDamageEvent.Pre}）
     */
    public static float calculateMainDamage(DamageContext ctx, float damage) {
        return ctx.getHandler().calculate(ctx, damage);
    }

    /**
     * 插槽 D：命中判定 / 无敌帧。
     *
     * @return {@code false} 表示丢弃本次伤害，链路应直接返回 {@code false}
     */
    public static boolean handleGate(DamageContext ctx) {
        return ctx.getHandler().onGate(ctx);
    }

    /** 插槽 E：护甲耐久。 */
    public static void applyArmorDurability(DamageContext ctx) {
        ctx.getHandler().onArmorDurability(ctx);
    }

    /** 插槽 E2：{@code LivingDamageEvent.Pre} 之前的最后修正。 */
    public static void onDamagePre(DamageContext ctx) {
        ctx.getHandler().onDamagePre(ctx, ctx.getContainer());
    }

    /**
     * 插槽 F：落地。
     *
     * @param damage {@code LivingDamageEvent.Pre} 之后的伤害
     */
    public static void applyDamage(DamageContext ctx, float damage) {
        ctx.getHandler().apply(ctx, damage);
    }

    /** 插槽 G：受伤表现。 */
    public static void playFeedback(DamageContext ctx) {
        ctx.getHandler().playFeedback(ctx);
    }

    /** 插槽 H：免死。 */
    public static boolean tryPreventDeath(DamageContext ctx) {
        return ctx.getHandler().preventDeath(ctx);
    }

    /** 插槽 I：落地后的收尾。 */
    public static void onDamageApplied(DamageContext ctx) {
        ctx.getHandler().afterApplied(ctx);
    }

    private DamagePipeline() {}
}
