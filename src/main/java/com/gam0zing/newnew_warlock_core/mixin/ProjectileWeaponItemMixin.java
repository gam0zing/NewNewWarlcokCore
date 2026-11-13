package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 幻影箭矢（弓、弩）。
 * <p>弓与弩在装填/射出时都会经过 {@code ProjectileWeaponItem#draw -> useAmmo} 消耗箭矢。
 * 当「无限附魔无效」且按 {@code phantom_arrow} 概率命中时，这里改为不消耗箭矢，
 * 额外消耗耐久（受耐久附魔影响）：</p>
 * <ul>
 *   <li>弓：射出时判定，消耗 3 点耐久，本次射出的箭矢即刻减伤并不可拾取；</li>
 *   <li>弩：装填时判定，消耗 5 点耐久，并在弩上写入幻影标记（本次装填的箭矢不入包），
 *       射击时由 {@code CrossbowItemMixin} 把标记透传给箭矢并清理弩自身的标记。</li>
 * </ul>
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

    /** 本次幻影箭矢对应的箭矢栈（仅服务端主线程内有效） */
    @Unique
    private static final ThreadLocal<ItemStack> NEWNEW$PHANTOM_AMMO = new ThreadLocal<>();

    /** 幻影箭矢的基础伤害削弱比例 */
    private static final double PHANTOM_DAMAGE_PENALTY = 0.35D;

    @Inject(method = "useAmmo", at = @At("HEAD"), cancellable = true)
    private static void newnew$phantomArrow(
        ItemStack weapon, ItemStack ammo, LivingEntity shooter, boolean intangible, CallbackInfoReturnable<ItemStack> cir
    ) {
        if (intangible) {
            return;
        }
        boolean bow = weapon.getItem() instanceof BowItem;
        if (!bow && !(weapon.getItem() instanceof CrossbowItem)) {
            return;
        }
        if (!(shooter instanceof Player player) || !(shooter.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (shooter.hasInfiniteMaterials()) {
            return;
        }
        // 只对箭矢生效：烟花火箭等非箭矢弹药不参与幻影判定
        if (!(ammo.getItem() instanceof ArrowItem arrowItem)) {
            return;
        }
        if (arrowItem.isInfinite(ammo, weapon, shooter)) {
            return;
        }

        int needed = EnchantmentHelper.processAmmoUse(serverLevel, weapon, ammo, 1);
        if (needed <= 0 || needed > ammo.getCount()) {
            return;
        }

        double chance = ModAttributes.getValue(player, ModAttributes.PHANTOM_ARROW);
        if (chance <= 0.0D || player.getRandom().nextDouble() >= chance) {
            return;
        }

        ItemStack phantom = ammo.copyWithCount(1);
        if (bow) {
            NEWNEW$PHANTOM_AMMO.set(phantom);
            weapon.hurtAndBreak(3, shooter, shooter.getUsedItemHand());
        } else {
            PhantomArrowState.markCrossbow(weapon);
            weapon.hurtAndBreak(5, shooter, shooter.getUsedItemHand());
        }
        cir.setReturnValue(phantom);
    }

    @Inject(method = "customArrow", at = @At("RETURN"))
    private void newnew$makePhantomUnpickable(
        AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack, CallbackInfoReturnable<AbstractArrow> cir
    ) {
        // 弓：按弹药栈引用命中；弩：按线程内标记命中
        boolean bowPhantom = projectileStack == NEWNEW$PHANTOM_AMMO.get();
        if (!bowPhantom && !PhantomArrowState.isActive()) {
            return;
        }
        if (bowPhantom) {
            NEWNEW$PHANTOM_AMMO.remove();
        }

        AbstractArrow result = cir.getReturnValue();
        if (result == null) {
            return;
        }
        // 弓与弩的幻影箭矢都不可拾取
        result.pickup = AbstractArrow.Pickup.DISALLOWED;

        // 幻影箭矢的基础伤害降低 35%
        AbstractArrowAccessor accessor = (AbstractArrowAccessor) (Object) result;
        accessor.newnew$setBaseDamage(accessor.newnew$getBaseDamage() * (1.0D - PHANTOM_DAMAGE_PENALTY));
    }
}
