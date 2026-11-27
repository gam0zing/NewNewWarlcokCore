package com.gam0zing.newnew_warlock_core.mixin.neoorigins;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cyberday1.neoorigins.service.ActiveOriginService;
import com.gam0zing.newnew_warlock_core.compat.neoorigins.OriginPowerQuery;

import net.minecraft.server.level.ServerPlayer;

/**
 * 让 NeoOrigins 的类型查询也能看到复合能力的子能力。
 * <p>只替换查询入口，不修改 allPowers / originActive，避免重复 tick 与生命周期变化。</p>
 */
@Mixin(ActiveOriginService.class)
public abstract class ActiveOriginServiceCompositeMixin {

    @Inject(method = "forEachOfType", at = @At("HEAD"), cancellable = true)
    private static void newnew$forEachOfType(
        ServerPlayer player, Class<?> type, Consumer<?> consumer, CallbackInfo callback) {
        OriginPowerQuery.forEachOfType(player, type, consumer);
        callback.cancel();
    }

    @Inject(method = "forEachOfTypeActive", at = @At("HEAD"), cancellable = true)
    private static void newnew$forEachOfTypeActive(
        ServerPlayer player, Class<?> type, Consumer<?> consumer, CallbackInfo callback) {
        OriginPowerQuery.forEachOfTypeActive(player, type, consumer);
        callback.cancel();
    }

    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private static void newnew$has(
        ServerPlayer player, Class<?> type, Predicate<?> predicate,
        CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(OriginPowerQuery.has(player, type, predicate));
        callback.cancel();
    }
}
