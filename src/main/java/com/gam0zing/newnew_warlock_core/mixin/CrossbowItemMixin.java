package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;
import com.gam0zing.newnew_warlock_core.share.PhantomArrowState;

/**
 * 蓄力速度（弩）与幻影箭矢（弩）。
 * <p>弩的蓄力时长由 {@code CrossbowItem#getChargeDuration} 决定（原版 1.25 秒），
 * 这里把返回的时长除以 {@code charge_speed}，客户端与服务端表现一致。</p>
 * <p>幻影箭矢（弩）：判定发生在装填阶段（见 {@code ProjectileWeaponItemMixin#useAmmo}），
 * 命中时不消耗箭矢并给弩写入标记。射击时把标记透传给本次射出的箭矢（减伤 + 不可拾取），
 * 同时清理弩自身的标记；弩仍会被正常清空，因此每次射击后都要重新装填。</p>
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void newnew$scaleChargeDuration(ItemStack crossbow, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (!(user instanceof Player player)) {
            return;
        }

        float scale = ModAttributes.getScale(player, ModAttributes.CHARGE_SPEED);
        if (scale == 1.0F) {
            return;
        }

        cir.setReturnValue(Math.max(1, Mth.floor(cir.getReturnValueI() / scale)));
    }

    @Inject(method = "performShooting", at = @At("HEAD"))
    private void newnew$propagatePhantomMark(
        Level level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        float power,
        float uncertainty,
        @Nullable LivingEntity targetOverride,
        CallbackInfo ci
    ) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!PhantomArrowState.isCrossbowMarked(weapon)) {
            return;
        }

        // 透传给本次射出的箭矢，并清理弩自身的标记
        PhantomArrowState.clearCrossbowMark(weapon);
        PhantomArrowState.begin();
    }

    @Inject(method = "performShooting", at = @At("RETURN"))
    private void newnew$clearPhantomFlag(
        Level level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        float power,
        float uncertainty,
        @Nullable LivingEntity targetOverride,
        CallbackInfo ci
    ) {
        PhantomArrowState.end();
    }
}
