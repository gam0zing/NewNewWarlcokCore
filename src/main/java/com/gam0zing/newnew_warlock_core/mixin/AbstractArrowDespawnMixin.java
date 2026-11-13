package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * 不可拾取箭矢的存在时长。
 * <p>原版落地箭矢需 1200 tick（60 秒）才会消失。这里把所有不可拾取
 * （{@code pickup != ALLOWED}，含 {@code DISALLOWED} 与 {@code CREATIVE_ONLY}）的箭矢缩短为 3 秒，
 * 避免幻影箭矢、无限附魔箭矢与怪物箭矢堆积。</p>
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowDespawnMixin {

    /** 不可拾取箭矢落地后的存在时长（tick） */
    @Unique
    private static final int NEWNEW$UNPICKABLE_LIFETIME = 60;

    @Shadow
    private int life;

    @Inject(method = "tickDespawn", at = @At("HEAD"), cancellable = true)
    private void newnew$shortenUnpickableLifetime(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.pickup == AbstractArrow.Pickup.ALLOWED) {
            return;
        }

        this.life++;
        if (this.life >= NEWNEW$UNPICKABLE_LIFETIME) {
            self.discard();
        }
        ci.cancel();
    }
}
