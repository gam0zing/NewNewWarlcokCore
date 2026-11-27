package com.gam0zing.newnew_warlock_core.mixin.neoorigins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cyberday1.neoorigins.api.NeoOriginsAPI;
import com.gam0zing.newnew_warlock_core.compat.neoorigins.OriginPowerQuery;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 让 {@code NeoOriginsAPI.hasPower} 也能命中复合能力的子能力。
 * <p>仅在原查询为 false 时递归展开，避免影响正常路径的性能。</p>
 */
@Mixin(NeoOriginsAPI.class)
public abstract class NeoOriginsAPIHasPowerMixin {

    @Inject(method = "hasPower", at = @At("RETURN"), cancellable = true)
    private static void newnew$hasCompositeChild(
        ServerPlayer player, Identifier power, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && OriginPowerQuery.hasPower(player, power)) {
            callback.setReturnValue(true);
        }
    }
}
