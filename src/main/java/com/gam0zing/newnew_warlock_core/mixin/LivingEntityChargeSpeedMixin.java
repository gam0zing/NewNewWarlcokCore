package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 蓄力速度（弓的表现与自动扳机判定）。
 * <p>弓的拉动进度由 {@code LivingEntity#getUseItemRemainingTicks()} 反推的
 * {@code getTicksUsingItem()} 提供，第一人称拉弓动画与 {@code PlayerAutoFireMixin} 都读取它。
 * 这里在使用弓时把返回值乘以 {@code charge_speed}，使表现与 {@code BowItemMixin} 修正后的实际威力同步。</p>
 * <p>弩的蓄力进度由 {@code CrossbowItem#getChargeDuration} 决定，已在 {@code CrossbowItemMixin} 单独处理，
 * 因此这里只处理弓，避免重复缩放。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityChargeSpeedMixin {

    @Inject(method = "getTicksUsingItem()I", at = @At("RETURN"), cancellable = true)
    private void newnew$scaleBowTicks(CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.getUseItem().getItem() instanceof BowItem)) {
            return;
        }

        int ticks = cir.getReturnValue();
        if (ticks <= 0) {
            return;
        }

        float scale = ModAttributes.getScale(self, ModAttributes.CHARGE_SPEED);
        if (scale == 1.0F) {
            return;
        }

        cir.setReturnValue((int) Math.min(72000.0D, Math.round(ticks * (double) scale)));
    }
}
