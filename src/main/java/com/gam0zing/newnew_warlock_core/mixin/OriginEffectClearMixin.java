package com.gam0zing.newnew_warlock_core.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 统一的效果清除黑名单：牛奶与 {@code /effect clear} 都走
 * {@link LivingEntity#removeAllEffects()}，在这里保留起源提示效果。
 */
@Mixin(LivingEntity.class)
public abstract class OriginEffectClearMixin {

    @Inject(method = "removeAllEffects", at = @At("HEAD"), cancellable = true)
    private void newnew$keepOriginEffects(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }

        boolean removed = false;
        for (MobEffectInstance instance : List.copyOf(player.getActiveEffects())) {
            if (OriginTraits.isProtectedEffect(instance)) {
                continue;
            }
            removed |= player.removeEffect(instance.getEffect());
        }
        cir.setReturnValue(removed);
    }
}
