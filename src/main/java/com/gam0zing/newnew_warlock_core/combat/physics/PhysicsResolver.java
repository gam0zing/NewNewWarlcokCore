package com.gam0zing.newnew_warlock_core.combat.physics;

import org.jetbrains.annotations.Nullable;

/**
 * 物理伤害结算核心。
 * <p><b>公式实现已清空</b>，等待后续重写。当前逻辑仅保证：</p>
 * <ul>
 *     <li>无视护甲 / 甲伤为 0 / 无护甲：肉伤全额进入肉体，无吸收、无耐久损耗</li>
 *     <li>有护甲参与：返回 {@code armorSkipped=true}，伤害全额穿透护甲（stub 行为）</li>
 * </ul>
 * <p>本方法只计算、不修改物品耐久；耐久损耗由调用方在落地阶段通过
 * {@link PhysicsArmorState#applyDurabilityLoss} 扣除。</p>
 */
public final class PhysicsResolver {

    private PhysicsResolver() {}

    /**
     * 结算一次物理伤害（公式 stub，待重写）。
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

        // 无视护甲 / 没有甲伤 / 没有护甲：甲伤不参与结算，肉伤全额进入肉体
        if (ignoresArmor || armorDamage <= 0.0D || armor == null) {
            return new PhysicsHitResult(
                meatDamage, armorDamage, penetration,
                0.0F, (float) meat, 0.0F, 0.0F, 0.0F, false, true, null);
        }

        // TODO: 物理伤害公式待重写。当前 stub 让护甲"跳过"，伤害全额穿透。
        return new PhysicsHitResult(
            meatDamage, armorDamage, penetration,
            0.0F, (float) meat, 0.0F, 0.0F, 0.0F, false, true, armor);
    }
}
