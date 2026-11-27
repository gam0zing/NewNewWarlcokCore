package com.gam0zing.newnew_warlock_core.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.network.chat.Component;
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
        boolean kept = false;
        for (MobEffectInstance instance : List.copyOf(player.getActiveEffects())) {
            if (OriginTraits.isProtectedEffect(instance)) {
                kept = true;
                continue;
            }
            removed |= player.removeEffect(instance.getEffect());
        }
        if (kept && !player.isDeadOrDying()) {
            // 牛奶与 /effect clear 都会走到这里；提示效果被保留，避免看起来像命令失效
            player.sendSystemMessage(
                Component.translatable("origin.newnew_warlock_core.effect.clear_protected"));
        }
        cir.setReturnValue(removed);
    }
}
