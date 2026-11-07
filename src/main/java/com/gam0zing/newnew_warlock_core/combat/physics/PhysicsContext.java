package com.gam0zing.newnew_warlock_core.combat.physics;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.body.BodyPart;

/**
 * 一次物理伤害流程内共享的数据。
 * <p>在插槽 A（预处理）确定武器、受击部位与护甲，插槽 C（主伤害计算）填充结算结果，
 * 插槽 D（护甲耐久）再根据结果扣除耐久。</p>
 */
public final class PhysicsContext {

    private final PhysicsWeapon weapon;
    private final BodyPart bodyPart;
    @Nullable
    private final PhysicsArmorState armor;
    private final boolean ignoresArmor;
    private final double weight;
    @Nullable
    private PhysicsHitResult result;

    public PhysicsContext(
        PhysicsWeapon weapon,
        BodyPart bodyPart,
        @Nullable PhysicsArmorState armor,
        boolean ignoresArmor,
        double weight) {
        this.weapon = weapon;
        this.bodyPart = bodyPart;
        this.armor = armor;
        this.ignoresArmor = ignoresArmor;
        this.weight = weight;
    }

    public PhysicsWeapon weapon() {
        return this.weapon;
    }

    public BodyPart bodyPart() {
        return this.bodyPart;
    }

    /** {@return 被命中护甲的运行时状态；该部位没有可结算护甲时为 null} */
    @Nullable
    public PhysicsArmorState armor() {
        return this.armor;
    }

    /** {@return 本次伤害是否无视护甲} */
    public boolean ignoresArmor() {
        return this.ignoresArmor;
    }

    /**
     * {@return 目标的总物理重量（身体重量 + 全身护甲重量）}
     * <p>不论受击部位有没有护甲，都使用这个总重量参与传递伤害计算。</p>
     */
    public double weight() {
        return this.weight;
    }

    /** {@return 主伤害计算阶段得到的结算结果；尚未结算时为 null} */
    @Nullable
    public PhysicsHitResult result() {
        return this.result;
    }

    public void setResult(PhysicsHitResult result) {
        this.result = result;
    }
}
