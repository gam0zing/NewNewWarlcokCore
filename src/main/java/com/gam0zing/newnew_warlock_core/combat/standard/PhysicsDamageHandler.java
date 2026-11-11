package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.api.IDamageHandler;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsContext;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsDamage;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * 物理伤害处理器：{@code physics} 标签的伤害（徒手/武器/投掷物/摔落/挤压/爆炸冲击等）。
 * <p>物理上下文（武器档案、受击部位、护甲、重量）在插槽 A 解析后挂到 {@link DamageContext} 的附件表上，
 * 后续阶段从附件表取用，骨架本身不认识物理模型。</p>
 */
public final class PhysicsDamageHandler implements IDamageHandler {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "physics");

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
        PhysicsContext physics = PhysicsDamage.prepare(ctx);
        if (physics != null) {
            ctx.setAttachment(PhysicsContext.class, physics);
        }
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
        return PhysicsDamage.calculate(ctx, damage);
    }

    @Override
    public boolean onGate(DamageContext ctx) {
        return VanillaDamageUtils.onGate(ctx);
    }

    @Override
    public void onArmorDurability(DamageContext ctx) {
        PhysicsDamage.applyArmorDurability(ctx);
    }

    @Override
    public void onDamagePre(DamageContext ctx, DamageContainer container) {
        // 物理模型在插槽 C 已经结算完毕，Pre 事件之前不需要再改
    }

    @Override
    public void apply(DamageContext ctx, float damage) {
        float healthBefore = ctx.getTarget().getHealth();

        float dealt = VanillaDamageUtils.apply(ctx, damage);

        // 受击部位的附加效果按实际扣血概率触发
        PhysicsDamage.applyBodyEffects(ctx, dealt, healthBefore);

        // 插槽 F2：击退（强度 = 未被重量吸收的冲击量）
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
        PhysicsDamage.applyKnockback(ctx, dealtDamage);
    }
}
