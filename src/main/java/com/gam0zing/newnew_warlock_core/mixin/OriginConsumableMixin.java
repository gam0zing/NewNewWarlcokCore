package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

/**
 * 种族食物禁用：石巨人无法进食，天堂鸟无法进食肉类。
 * <p>从消耗动作入口直接拒绝，避免出现"能嚼但没效果"的误导；猫的素食仍然可用。</p>
 */
@Mixin(Consumable.class)
public abstract class OriginConsumableMixin {

    @Inject(method = "startConsuming", at = @At("HEAD"), cancellable = true)
    private void newnew$blockUnedible(
        LivingEntity user,
        ItemStack stack,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir) {
        if (user instanceof Player player && OriginTraits.blocksConsumable(player, stack)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
