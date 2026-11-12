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
 * 致命伤：持续效果，按「多份施加累加等级」的方式叠加（与出血同一套容器机制）。
 * <p>持续期间，目标任意伤害流程的扣血阶段结束后，若剩余生命值小于
 * {@code 最大生命值 × 15% × 效果等级} 且未死亡，则立刻由效果来源造成同样数值
 * 的流血伤害，并移除本效果（连同整个容器的所有子实例）。</p>
 * <p>等级取全部子实例的等级之和（容器模式），普通实例仍按 {@code amplifier + 1}。</p>
 * <p>流血伤害使用 {@code newnew_warlock_core:bleeding} 伤害类型（属于 {@code inner_damage}），
 * 按内部伤害直接扣血，不经过护甲、附魔与无敌帧。</p>
 */
public class FatalWoundMobEffect extends MobEffect {

    /** 触发阈值与伤害：每级为最大生命值的 15% */
    public static final float HEALTH_RATIO_PER_LEVEL = 0.15F;

    private static final ResourceKey<DamageType> DAMAGE_TYPE =
        ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "bleeding"));

    public FatalWoundMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * 任意伤害流程的扣血阶段结束后调用。
     * <p>命中条件：目标持有本效果、剩余生命值低于 {@code 最大生命值 × 15% × 等级}、且没有死亡。</p>
     *
     * @param target 刚刚扣除过生命值的生物
     */
    public static void afterHealthDeducted(LivingEntity target) {
        MobEffectInstance instance = target.getEffect(ModMobEffects.FATAL_WOUND);
        if (instance == null) {
            return;
        }

        int level = effectLevel(instance);
        float amount = target.getMaxHealth() * HEALTH_RATIO_PER_LEVEL * level;
        if (target.isDeadOrDying() || target.getHealth() >= amount) {
            return;
        }

        LivingEntity source = EffectSources.attributionSourceOf(instance, target);

        // 先移除效果再造成伤害，避免这次流血伤害再次触发本效果
        target.removeEffect(ModMobEffects.FATAL_WOUND);
        dealBleedingDamage(target, source, amount);
    }

    /**
     * {@return 本次结算使用的等级}
     * <p>容器模式下读取全部子实例的等级合计值，不受 {@code amplifier} 的 255 上限限制；
     * 普通实例仍按原版语义 {@code amplifier + 1} 计算。</p>
     */
    private static int effectLevel(MobEffectInstance instance) {
        if (instance instanceof EffectStack stack) {
            return Math.max(1, stack.totalLevel());
        }
        return instance.getAmplifier() + 1;
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
