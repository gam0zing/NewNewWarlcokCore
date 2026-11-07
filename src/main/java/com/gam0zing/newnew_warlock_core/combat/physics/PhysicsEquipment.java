package com.gam0zing.newnew_warlock_core.combat.physics;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.body.BodyPart;
import com.gam0zing.newnew_warlock_core.registry.ModDataComponents;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;

/**
 * 从伤害来源与受击目标身上采集物理档案。
 * <p>武器档案始终来自“这次攻击使用的物品”：</p>
 * <ul>
 *     <li>近战：攻击者主手物品</li>
 *     <li>箭/三叉戟等弹射物：弹射物关联的物品栈（{@link AbstractArrow#getPickupItemStackOrigin()}，
 *     三叉戟即三叉戟物品）</li>
 *     <li>其他实现 {@link ItemSupplier} 的弹射物（雪球、药水等）：{@link ItemSupplier#getItem()}</li>
 * </ul>
 * <p>护甲档案来自受击部位 {@code armor_slot} 指定的装备槽；部位没有槽位、槽位为空、
 * 或物品没有 {@code physics_armor} 组件时视为没有护甲。</p>
 */
public final class PhysicsEquipment {

    /** 参与物理重量汇总的护甲槽（只用于读重量，不影响“单槽命中判定”的简化规则） */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private PhysicsEquipment() {}

    /** {@return 本次伤害使用的物理武器档案} */
    public static Optional<PhysicsWeapon> weapon(DamageSource source) {
        ItemStack stack = weaponStack(source);
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.get(ModDataComponents.PHYSICS_WEAPON.get()));
    }

    /**
     * 一次遍历采集受击部位护甲与全身护甲重量。
     * <p>受击部位护甲来自部位 {@code armor_slot} 指定的装备槽；部位没有槽位、槽位为空、
     * 或物品没有 {@code physics_armor} 组件时视为没有护甲。重量则统计全部四个护甲槽，
     * 与受击部位是否被覆盖无关（没有档案的槽不计入）。</p>
     *
     * @param part   受击部位
     * @param target 受击生物
     * @return 受击部位护甲状态（可能为 null）与全身护甲重量之和
     */
    public static ArmorLoad armorLoad(BodyPart part, LivingEntity target) {
        EquipmentSlot hitSlot = part.armorSlot();
        PhysicsArmorState hitArmor = null;
        double totalWeight = 0.0D;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            PhysicsArmor profile = stack.get(ModDataComponents.PHYSICS_ARMOR.get());
            if (profile == null) {
                continue;
            }
            totalWeight += profile.weight();
            if (hitArmor == null && slot == hitSlot) {
                hitArmor = PhysicsArmorState.create(profile, stack, slot);
            }
        }
        return new ArmorLoad(hitArmor, Math.max(0.0D, totalWeight));
    }

    /**
     * 护甲采集结果。
     *
     * @param hitArmor    受击部位对应的护甲运行时状态；该部位没有可结算护甲时为 null
     * @param totalWeight 全身护甲重量之和（四个护甲槽，非负）
     */
    public record ArmorLoad(@Nullable PhysicsArmorState hitArmor, double totalWeight) {}

    /** {@return 这次攻击使用的物品栈；找不到时为 {@link ItemStack#EMPTY}} */
    private static ItemStack weaponStack(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();

        // 弹射物：优先取弹射物自身关联的物品（三叉戟、箭等）
        if (direct instanceof AbstractArrow arrow) {
            ItemStack origin = arrow.getPickupItemStackOrigin();
            if (!origin.isEmpty()) {
                return origin;
            }
        }
        if (direct instanceof ItemSupplier supplier) {
            ItemStack item = supplier.getItem();
            if (!item.isEmpty()) {
                return item;
            }
        }

        // 近战：直接来源的生物（玩家攻击时就是玩家本人）
        if (direct instanceof LivingEntity living) {
            return living.getMainHandItem();
        }
        if (causing instanceof LivingEntity living) {
            return living.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }
}
