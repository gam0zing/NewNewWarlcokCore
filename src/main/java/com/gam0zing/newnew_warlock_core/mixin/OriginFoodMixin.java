package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 种族食物规则：石巨人无法进食。
 * <p>食物营养与饱和度的改写在 {@link OriginFoodConsumeMixin} 中处理。</p>
 */
@Mixin(Player.class)
public abstract class OriginFoodMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void newnew$blockEating(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer player && OriginTraits.blocksEating(player)) {
            cir.setReturnValue(false);
        }
    }
}
