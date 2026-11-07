package com.gam0zing.newnew_warlock_core.combat.physics;

import org.jetbrains.annotations.Nullable;

/**
 * 一次物理结算的结果（对应参考项目里 C# 的 {@code DamageResult}）。
 *
 * @param meatDamageInput  输入肉伤（原始伤害 × 身体部位刚性档位倍率）
 * @param armorDamageInput 输入甲伤（原始伤害 × 护甲刚性档位倍率）
 * @param penetration      本击穿透
 * @param armorVolume      护甲实际吸收的甲伤
 * @param directMeatDamage 穿透护甲后直接进入肉体部分的肉伤
 * @param knockDamage      护甲吸收冲击后以“震动”形式传递到肉体的肉伤
 * @param durabilityLoss   本击造成的护甲耐久损耗
 * @param broke            本击是否击破护甲
 * @param armorSkipped     是否跳过了护甲（无视护甲、没有甲伤、或没有可结算护甲）
 * @param armor            本次参与结算的护甲运行时状态（跳过护甲时为 null）
 */
public record PhysicsHitResult(
    float meatDamageInput,
    float armorDamageInput,
    float penetration,
    float armorVolume,
    float directMeatDamage,
    float knockDamage,
    float durabilityLoss,
    boolean broke,
    boolean armorSkipped,
    @Nullable PhysicsArmorState armor) {

    /** {@return 本次实际进入肉体的总伤害（直接部分 + 震动传递部分）} */
    public float meatDamage() {
        return this.directMeatDamage + this.knockDamage;
    }
}
