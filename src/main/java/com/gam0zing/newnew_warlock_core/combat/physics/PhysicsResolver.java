package com.gam0zing.newnew_warlock_core.combat.physics;

import org.jetbrains.annotations.Nullable;

/**
 * 物理伤害结算核心。
 * <p>公式与参考项目 {@code damageSysTest} 的 {@code PhysicsDamageResolver.Resolve} 完全一致，
 * 只是把 C# 的“武器 + 护甲”入口拆成了 Java 侧的三元组 {@code (肉伤, 穿透, 甲伤)}，
 * 并去掉了 C# 的 hit/sustain 分支（两者的结算逻辑本来就相同）。</p>
 *
 * <p>结算顺序：</p>
 * <ol>
 *     <li>击破判定：{@code 甲伤 × 穿透 > 防护力 × 刚性}</li>
 *     <li>护甲强度系数：未击破 {@code 刚性 / √穿透}，已击破 {@code 0.05 + 0.95 × T/(T+1)}</li>
 *     <li>护甲厚度：{@code 防护力 × 护甲强度系数}</li>
 *     <li>护甲吸收体积：{@code min(甲伤, 厚度/穿透, 剩余耐久 × 护甲强度系数)}</li>
 *     <li>肉伤拆分：直接部分 {@code 肉伤 × (1 - 吸收体积/甲伤)}，震动部分按缓冲性/重量/穿透折算</li>
 *     <li>耐久损耗：{@code 吸收体积 / 护甲强度系数}；已击破时额外损耗 {@code 甲伤 × (1 - 0.75×T/(T+1))}</li>
 * </ol>
 * <p>本方法只计算、不修改物品耐久；耐久损耗由调用方在落地阶段通过
 * {@link PhysicsArmorState#applyDurabilityLoss} 扣除。</p>
 */
public final class PhysicsResolver {

    /** 穿透下限，避免除以 0 */
    private static final double MIN_PENETRATION = 0.01D;

    private PhysicsResolver() {}

    /**
     * 结算一次物理伤害。
     *
     * @param meatDamage   肉伤（已按身体部位刚性档位倍率折算）
     * @param penetration  穿透
     * @param armorDamage  甲伤（已按护甲刚性档位倍率折算）
     * @param ignoresArmor 是否无视护甲（伤害类型的 {@code bypasses_armor} 标签）
     * @param weight       目标的总物理重量（身体重量 + 全身护甲重量），用于传递伤害折算
     * @param armor        护甲运行时状态；没有可结算护甲时为 null
     */
    public static PhysicsHitResult resolve(
        float meatDamage,
        float penetration,
        float armorDamage,
        boolean ignoresArmor,
        double weight,
        @Nullable PhysicsArmorState armor) {

        double meat = Math.max(0.0D, meatDamage);
        double armorDmg = Math.max(0.0D, armorDamage);

        // 无视护甲 / 没有甲伤 / 没有护甲：甲伤不参与结算，肉伤全额进入肉体
        if (ignoresArmor || armorDmg <= 0.0D || armor == null) {
            return new PhysicsHitResult(
                meatDamage, armorDamage, penetration,
                0.0F, (float) meat, 0.0F, 0.0F, false, true, null);
        }

        double safePenetration = Math.max(penetration, MIN_PENETRATION);
        double baseProtection = armor.protection();

        double damageThickness = armorDmg * safePenetration;
        boolean broke = damageThickness > baseProtection * armor.rigidity();

        double armorPower = broke
            ? armor.postBreakProtectionRatio()
            : armor.rigidity() / Math.sqrt(safePenetration);
        double armorThickness = baseProtection * armorPower;

        double armorVolume = 0.0D;
        if (armorThickness > 0.0D && armorPower > 0.0D && armor.durability() > 0.0D) {
            double maxByProtection = armorThickness / safePenetration;
            double maxByDurability = armor.durability() * armorPower;
            armorVolume = Math.min(armorDmg, Math.min(maxByProtection, maxByDurability));
        }

        double ratioInArmor = armorVolume / armorDmg;
        double ratioInMeat = 1.0D - ratioInArmor;

        double durabilityLoss = 0.0D;
        double knockDamage = 0.0D;

        if (armorThickness > 0.0D && armorPower > 0.0D) {
            durabilityLoss = armorVolume / armorPower;

            // 参考项目：总重量越大，传递越明显；缓冲性降低传递
            double safeWeight = Math.max(0.0D, weight);
            double weightFactor = safeWeight / (safeWeight + 10.0D);
            double weightDenominator = 1.0D - weightFactor;
            if (weightDenominator > 1.0E-9D) {
                knockDamage = Math.max(armorVolume - durabilityLoss, 0.0D)
                    * armor.cushionProtectionRatio()
                    / weightDenominator
                    * Math.sqrt(Math.max(0.0D, penetration));
            }

            if (broke) {
                durabilityLoss += armorDmg * armor.postBreakDurabilityMultiplier();
            }
        }

        double directMeatDamage = meat * ratioInMeat;

        return new PhysicsHitResult(
            meatDamage,
            armorDamage,
            penetration,
            (float) armorVolume,
            (float) directMeatDamage,
            (float) knockDamage,
            (float) durabilityLoss,
            broke,
            false,
            armor);
    }
}
