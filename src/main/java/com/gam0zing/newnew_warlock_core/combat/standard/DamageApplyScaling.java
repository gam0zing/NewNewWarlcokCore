package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * 扣血阶段的属性修正。
 * <p>作用点是 {@code LivingDamageEvent.Pre} 之后、真正从生命值里扣血之前，
 * 因此以下倍率影响的是「本次实际扣掉多少血」，与伤害类型、护甲减免等前置环节无关：</p>
 * <ul>
 *     <li>{@code projectile_damage_scale}：攻击者的弹射物伤害倍率，只作用于弹射物标签的伤害；</li>
 *     <li>{@code critical} / {@code critical_damage}：攻击者的暴击率与暴击伤害；</li>
 *     <li>{@code damage_distance_fix}：攻击者的伤害距离修正，相距 8 格为零点按距离线性变化；</li>
 *     <li>{@code unarmored_slayer}：目标没穿任何护甲时攻击者的伤害倍率；</li>
 *     <li>{@code giant_slayer} / {@code dwarf_slayer}：按双方 AABB 体积大小关系施加「大出血」/「致命伤」；</li>
 *     <li>{@code rock_skin}：受击者扣除血量前减去的固定值；</li>
 *     <li>{@code hurt_scale}：受击者的受伤倍率；</li>
 *     <li>{@code hurt_limit}：受击者单次扣血上限（最大生命值的比例）。</li>
 * </ul>
 */
public final class DamageApplyScaling {

    /** 巨人/侏儒杀手施加效果的持续时间（刻）；大出血是瞬时效果，时长无意义 */
    private static final int SLAYER_EFFECT_DURATION = 200;

    /** 伤害距离修正的零点距离（格）：相距 8 格时修正为 0 */
    private static final double DISTANCE_ZERO_POINT = 8.0D;

    /** 无甲杀手检查的人形护甲槽位 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * 扣血前修正伤害。
     *
     * @param damage {@code LivingDamageEvent.Pre} 之后的伤害
     * @return 实际要扣除的血量
     */
    public static float apply(DamageContext ctx, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }

        LivingEntity target = ctx.getTarget();
        LivingEntity attacker = ctx.getAttacker();
        float result = damage;

        if (attacker != null) {
            if (ctx.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                result *= ModAttributes.getScale(attacker, ModAttributes.PROJECTILE_DAMAGE_SCALE);
            }
            result = applyCritical(attacker, result);
            result = applyDistanceFix(attacker, target, result);
            result = applyUnarmoredSlayer(attacker, target, result);
            applySlayerEffects(attacker, target);
        }

        result = Math.max(0.0F, result - (float) ModAttributes.getValue(target, ModAttributes.ROCK_SKIN));
        result *= ModAttributes.getScale(target, ModAttributes.HURT_SCALE);

        double hurtLimit = ModAttributes.getValue(target, ModAttributes.HURT_LIMIT);
        if (hurtLimit > 0.0D) {
            result = Math.min(result, (float) (target.getMaxHealth() * (1.0D - hurtLimit)));
        }
        return Math.max(0.0F, result);
    }

    /**
     * 暴击：{@code critical} 的整数部分为必定暴击的层数，小数部分为是否再叠加一层的概率；
     * 每层把扣血量乘以 {@code critical_damage}。
     */
    private static float applyCritical(LivingEntity attacker, float damage) {
        double critical = ModAttributes.getValue(attacker, ModAttributes.CRITICAL);
        if (critical <= 0.0D) {
            return damage;
        }

        int stacks = (int) Math.floor(critical);
        double extraChance = critical - stacks;
        if (extraChance > 0.0D && attacker.getRandom().nextDouble() < extraChance) {
            stacks++;
        }
        if (stacks <= 0) {
            return damage;
        }

        double criticalDamage = ModAttributes.getValue(attacker, ModAttributes.CRITICAL_DAMAGE);
        return (float) (damage * Math.pow(criticalDamage, stacks));
    }

    /** 伤害距离修正：双方每远离 1 格扣血量变化该百分比，相距 8 格为零点。 */
    private static float applyDistanceFix(LivingEntity attacker, LivingEntity target, float damage) {
        double fix = ModAttributes.getValue(attacker, ModAttributes.DAMAGE_DISTANCE_FIX);
        if (fix == 0.0D) {
            return damage;
        }
        double multiplier = 1.0D + fix * (attacker.distanceTo(target) - DISTANCE_ZERO_POINT);
        return (float) (damage * Math.max(0.0D, multiplier));
    }

    /** 无甲杀手：目标没穿任何护甲时伤害乘以攻击者的 {@code unarmored_slayer}。 */
    private static float applyUnarmoredSlayer(LivingEntity attacker, LivingEntity target, float damage) {
        float scale = ModAttributes.getScale(attacker, ModAttributes.UNARMORED_SLAYER);
        if (scale == 1.0F) {
            return damage;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!target.getItemBySlot(slot).isEmpty()) {
                return damage;
            }
        }
        return damage * scale;
    }

    /** 巨人杀手 / 侏儒杀手：按双方 AABB 体积大小关系施加「大出血」/「致命伤」，相等时都不触发。 */
    private static void applySlayerEffects(LivingEntity attacker, LivingEntity target) {
        double attackerVolume = VanillaDamageUtils.boundingBoxVolume(attacker);
        double targetVolume = VanillaDamageUtils.boundingBoxVolume(target);
        if (targetVolume > attackerVolume) {
            int level = rollLevel(ModAttributes.getValue(attacker, ModAttributes.GIANT_SLAYER), attacker.getRandom());
            if (level > 0) {
                EffectSources.apply(target,
                    new MobEffectInstance(ModMobEffects.HEMORRHAGE, 1, level - 1), attacker);
            }
        } else if (attackerVolume > targetVolume) {
            int level = rollLevel(ModAttributes.getValue(attacker, ModAttributes.DWARF_SLAYER), attacker.getRandom());
            if (level > 0) {
                EffectSources.apply(target,
                    new MobEffectInstance(ModMobEffects.FATAL_WOUND, SLAYER_EFFECT_DURATION, level - 1), attacker);
            }
        }
    }

    /** {@return 数值等级：整数部分保底，未满 1 的部分按概率再升一级} */
    private static int rollLevel(double value, RandomSource random) {
        if (value <= 0.0D) {
            return 0;
        }
        int level = (int) Math.floor(value);
        if (random.nextDouble() < value - level) {
            level++;
        }
        return level;
    }

    private DamageApplyScaling() {}
}
