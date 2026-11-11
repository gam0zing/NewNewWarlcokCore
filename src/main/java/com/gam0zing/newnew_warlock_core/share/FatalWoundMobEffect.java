package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

/**
 * 致命伤：持续效果。
 * <p>持续期间，目标任意伤害流程的扣血阶段结束后，若剩余生命值小于
 * {@code 6 × 效果等级} 且未死亡，则立刻由效果来源造成 {@code 6 × 效果等级}
 * 的流血伤害，并移除本效果。</p>
 * <p>流血伤害使用 {@code newnew_warlock_core:bleeding} 伤害类型（属于 {@code inner_damage}），
 * 按内部伤害直接扣血，不经过护甲、附魔与无敌帧。</p>
 */
public class FatalWoundMobEffect extends MobEffect {

    /** 触发阈值：每级 6 点生命值 */
    public static final float THRESHOLD_PER_LEVEL = 6.0F;
    /** 触发伤害：每级 6 点流血伤害 */
    public static final float DAMAGE_PER_LEVEL = 6.0F;

    private static final ResourceKey<DamageType> DAMAGE_TYPE =
        ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "bleeding"));

    public FatalWoundMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * 任意伤害流程的扣血阶段结束后调用。
     * <p>命中条件：目标持有本效果、剩余生命值低于 {@code 6 × 等级}、且没有死亡。</p>
     *
     * @param target 刚刚扣除过生命值的生物
     */
    public static void afterHealthDeducted(LivingEntity target) {
        MobEffectInstance instance = target.getEffect(ModMobEffects.FATAL_WOUND);
        if (instance == null) {
            return;
        }

        int level = instance.getAmplifier() + 1;
        if (target.isDeadOrDying() || target.getHealth() >= THRESHOLD_PER_LEVEL * level) {
            return;
        }

        LivingEntity source = EffectSources.resolvedSourceOf(instance, target);

        // 先移除效果再造成伤害，避免这次流血伤害再次触发本效果
        target.removeEffect(ModMobEffects.FATAL_WOUND);
        dealBleedingDamage(target, source, DAMAGE_PER_LEVEL * level);
    }

    /** 对目标造成一次流血伤害，来源为施加本效果的生物（可为 null）。 */
    private static void dealBleedingDamage(LivingEntity target, @Nullable LivingEntity source, float damage) {
        Holder<DamageType> type = target.damageSources().damageTypes.get(DAMAGE_TYPE).orElse(null);
        if (type == null) {
            NewNewWarlockCore.LOGGER.warn("[fatal_wound] 伤害类型 {} 未加载，本次流血伤害未结算", DAMAGE_TYPE.identifier());
            return;
        }
        target.hurt(new DamageSource(type, null, source, target.position()), damage);
    }
}
