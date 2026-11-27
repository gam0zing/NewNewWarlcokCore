package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.client.ModelColorSkinTint;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * 在客户端获取玩家皮肤时套用 model_color 染色。
 * <p>优先级高于 NeoOrigins 的 morph 皮肤 mixin，确保染色作用在最终皮肤上。</p>
 */
@Mixin(value = AbstractClientPlayer.class, priority = 2000)
public abstract class PlayerSkinTintMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void newnew$applyModelColor(CallbackInfoReturnable<PlayerSkin> callback) {
        PlayerSkin original = callback.getReturnValue();
        if (original == null) {
            return;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        try {
            PlayerSkin tinted = ModelColorSkinTint.apply(player.getUUID(), original);
            if (tinted != original) {
                callback.setReturnValue(tinted);
            }
        } catch (RuntimeException ignored) {
            // 渲染路径不应因染色失败而中断。
        }
    }
}
