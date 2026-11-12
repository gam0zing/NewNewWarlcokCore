package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.api.IDamageHandler;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageCategory;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * 原版伤害行为的处理器：没有命中任何专用标签的伤害都走这里。
 * <p>每个阶段都只是转调 {@link VanillaDamageUtils}，因此“原版怎么处理”与“处理器怎么分派”是分开的，
 * 后续替换某一段规则时只需要改工具类或换处理器，不需要动骨架。</p>
 */
public final class VanillaDamageHandler implements IDamageHandler {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "vanilla");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public boolean isInner(DamageContext ctx) {
        return false;
    }

    @Override
    public void prepare(DamageContext ctx) {
        VanillaDamageUtils.prepare(ctx);
    }

    @Override
    public void onShieldBlock(DamageContext ctx) {
        VanillaDamageUtils.onShieldBlock(ctx);
    }

    @Override
    public void onShieldDurability(DamageContext ctx) {
        VanillaDamageUtils.onShieldDurability(ctx);
    }

    @Override
    public float calculate(DamageContext ctx, float damage) {
        return VanillaDamageUtils.calculate(ctx, damage);
    }

    @Override
    public boolean onGate(DamageContext ctx) {
        return VanillaDamageUtils.onGate(ctx);
    }

    @Override
    public void onArmorDurability(DamageContext ctx) {
        VanillaDamageUtils.onArmorDurability(ctx);
    }

    @Override
    public void onDamagePre(DamageContext ctx, DamageContainer container) {
        // 原版不在这里做任何事
    }

    @Override
    public void apply(DamageContext ctx, float damage) {
        float dealt = VanillaDamageUtils.apply(ctx, damage);
        // 插槽 F2：物理基础击退（强度 sqrt(伤害 / 10)）
        this.applyKnockback(ctx, dealt);
    }

    @Override
    public void playFeedback(DamageContext ctx) {
        VanillaCombatViewHelper.playHurtFeedback(ctx);
    }

    @Override
    public boolean preventDeath(DamageContext ctx) {
        // TODO: 图腾 / 免死效果（重发 LivingUseTotemEvent）
        return false;
    }

    @Override
    public void afterApplied(DamageContext ctx) {
        // TODO: 状态效果回调、统计、进度、食物消耗
    }

    @Override
    public void applyKnockback(DamageContext ctx, float dealtDamage) {
        // 击退只对物理伤害生效
        if (ctx.getCategory() != DamageCategory.PHYSICS) {
            return;
        }
        VanillaDamageUtils.dealDefaultKnockback(ctx, dealtDamage);
    }
}
