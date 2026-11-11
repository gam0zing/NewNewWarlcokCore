package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.api.IDamageHandler;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.spine.InnerDamage;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * 内部伤害处理器：{@code inner_damage} 标签的伤害（出血、溺水、饥饿、脱水等）。
 * <p>在插槽 0 直接扣除生命值并结束流程：不经过防护、加成、无敌帧与伤害事件。
 * 其余阶段不可达，因此显式抛出异常，避免有人误以为“内部伤害也会走这些阶段”。</p>
 */
public final class InnerDamageHandler implements IDamageHandler {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "inner");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public boolean isInner(DamageContext ctx) {
        ctx.setDealtDamage(InnerDamage.apply(ctx.getTarget(), ctx.getSource(), ctx.getIncomingDamage()));
        return true;
    }

    @Override
    public void prepare(DamageContext ctx) {
        throw unreachable("prepare");
    }

    @Override
    public void onShieldBlock(DamageContext ctx) {
        throw unreachable("onShieldBlock");
    }

    @Override
    public void onShieldDurability(DamageContext ctx) {
        throw unreachable("onShieldDurability");
    }

    @Override
    public float calculate(DamageContext ctx, float damage) {
        throw unreachable("calculate");
    }

    @Override
    public boolean onGate(DamageContext ctx) {
        throw unreachable("onGate");
    }

    @Override
    public void onArmorDurability(DamageContext ctx) {
        throw unreachable("onArmorDurability");
    }

    @Override
    public void onDamagePre(DamageContext ctx, DamageContainer container) {
        throw unreachable("onDamagePre");
    }

    @Override
    public void apply(DamageContext ctx, float damage) {
        throw unreachable("apply");
    }

    @Override
    public void playFeedback(DamageContext ctx) {
        throw unreachable("playFeedback");
    }

    @Override
    public boolean preventDeath(DamageContext ctx) {
        throw unreachable("preventDeath");
    }

    @Override
    public void afterApplied(DamageContext ctx) {
        throw unreachable("afterApplied");
    }

    @Override
    public void applyKnockback(DamageContext ctx, float dealtDamage) {
        throw unreachable("applyKnockback");
    }

    private static IllegalStateException unreachable(String stage) {
        return new IllegalStateException("内部伤害在插槽 0 已结束，不应进入 " + stage);
    }
}
