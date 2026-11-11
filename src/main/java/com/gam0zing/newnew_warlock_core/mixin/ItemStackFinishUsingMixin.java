package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gam0zing.newnew_warlock_core.share.EffectSources;

/**
 * 直接进食/饮用水路。
 * <p>26.3 移除了 {@code LivingEntity#eat}，食物与药水的效果统一在
 * {@link ItemStack#finishUsingItem(Level, LivingEntity)} 里结算（狐狸等绕过
 * {@code completeUsingItem} 的路径也走这里）。</p>
 * <p>只把“使用者本人”登记为来源；与 {@code completeUsingItem} 嵌套时按栈配对，
 * 不会改变原有归因。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackFinishUsingMixin {

    @Inject(
        method = "finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)"
            + "Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"))
    private void newnew$beginSelfConsumption(Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        EffectSources.beginSelfConsumption(user);
    }

    @Inject(
        method = "finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)"
            + "Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"))
    private void newnew$endSelfConsumption(Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        EffectSources.endSelfConsumption();
    }
}
