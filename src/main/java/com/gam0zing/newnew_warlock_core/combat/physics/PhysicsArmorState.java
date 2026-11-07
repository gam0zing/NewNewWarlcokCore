package com.gam0zing.newnew_warlock_core.combat.physics;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * 一次结算中某件护甲的运行时快照。
 * <p>把 {@link PhysicsArmor} 档案与实际的 {@link ItemStack}（耐久）结合起来，得到本次结算需要的动态值：
 * 耐久比例、剩余耐久、当前防护力、原版盔甲值、重量，以及只依赖刚性和韧性的缓存比例。</p>
 * <p>不可破坏装备（{@code getMaxDamage() == 0}）的耐久比例按 1.0 处理，剩余耐久视为无限。</p>
 */
public final class PhysicsArmorState {

    private final PhysicsArmor profile;
    private final ItemStack stack;
    private final EquipmentSlot slot;
    private final double durabilityRatio;
    private final double durability;
    private final double armorValue;
    private final double protection;
    private final double weight;

    private PhysicsArmorState(
        PhysicsArmor profile, ItemStack stack, EquipmentSlot slot,
        double durabilityRatio, double durability, double armorValue) {
        this.profile = profile;
        this.stack = stack;
        this.slot = slot;
        this.durabilityRatio = durabilityRatio;
        this.durability = durability;
        this.armorValue = armorValue;
        this.protection = profile.protection(durabilityRatio, armorValue);
        this.weight = profile.weight();
    }

    /**
     * 由档案与物品栈创建运行时快照。
     *
     * @param profile 护甲档案
     * @param stack   护甲物品栈（调用方保证非空且带档案）
     * @param slot    物品所在的装备槽
     */
    public static PhysicsArmorState create(PhysicsArmor profile, ItemStack stack, EquipmentSlot slot) {
        double armorValue = vanillaArmorValue(stack, slot);
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return new PhysicsArmorState(profile, stack, slot, 1.0D, Double.MAX_VALUE, armorValue);
        }
        int damageValue = Math.min(maxDamage, Math.max(0, stack.getDamageValue()));
        double ratio = (double) (maxDamage - damageValue) / (double) maxDamage;
        return new PhysicsArmorState(
            profile, stack, slot, ratio, (double) (maxDamage - damageValue), armorValue);
    }

    /** {@return 物品在该装备槽提供的原版盔甲值；没有盔甲修饰符时为 0} */
    private static double vanillaArmorValue(ItemStack stack, EquipmentSlot slot) {
        double[] total = {0.0D};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ARMOR)) {
                total[0] += modifier.amount();
            }
        });
        return Math.max(0.0D, total[0]);
    }

    public PhysicsArmor profile() {
        return this.profile;
    }

    public ItemStack stack() {
        return this.stack;
    }

    public EquipmentSlot slot() {
        return this.slot;
    }

    /** {@return 耐久比例，0-1} */
    public double durabilityRatio() {
        return this.durabilityRatio;
    }

    /** {@return 剩余耐久（不可破坏装备为 {@link Double#MAX_VALUE}）} */
    public double durability() {
        return this.durability;
    }

    /** {@return 当前防护力} */
    public double protection() {
        return this.protection;
    }

    /** {@return 物品的原版盔甲值} */
    public double armorValue() {
        return this.armorValue;
    }

    /** {@return 重量} */
    public double weight() {
        return this.weight;
    }

    /** {@return 刚性（非负）} */
    public double rigidity() {
        return this.profile.rigidity().nonNegativeValue();
    }

    /** {@return 韧性对应的抗破防系数 {@code T / (T + 1)}} */
    public double toughnessFactor() {
        double toughness = this.profile.toughness().nonNegativeValue();
        return toughness / (toughness + 1.0D);
    }

    /** {@return 击破后的防护力比例 {@code 0.05 + 0.95 * T/(T+1)}} */
    public double postBreakProtectionRatio() {
        return 0.05D + 0.95D * toughnessFactor();
    }

    /** {@return 击破后的额外耐久损耗比例 {@code 1 - 0.75 * T/(T+1)}} */
    public double postBreakDurabilityMultiplier() {
        return 1.0D - 0.75D * toughnessFactor();
    }

    /** {@return 缓冲性对应的传递伤害比例 {@code 1 - cushioning/(cushioning+1)}} */
    public double cushionProtectionRatio() {
        double cushioning = this.profile.cushioning().nonNegativeValue();
        return 1.0D - cushioning / (cushioning + 1.0D);
    }

    /** {@return 是否不可破坏（不扣耐久）} */
    public boolean unbreakable() {
        return this.stack.getMaxDamage() <= 0;
    }

    /**
     * 把结算出的耐久损耗实际扣到物品上。
     * <p>不可破坏装备直接忽略；损耗按四舍五入转为整数耐久点，至少 1 点。
     * 走 {@link ItemStack#hurtAndBreak} 以保留损坏事件、音效与其他模组的钩子。</p>
     *
     * @param loss  耐久损耗（与 {@link #durability()} 同单位）
     * @param owner 护甲穿戴者
     */
    public void applyDurabilityLoss(double loss, LivingEntity owner) {
        if (!(loss > 0.0D) || unbreakable()) {
            return;
        }
        int points = Math.max(1, (int) Math.round(loss));
        this.stack.hurtAndBreak(points, owner, this.slot);
    }
}
