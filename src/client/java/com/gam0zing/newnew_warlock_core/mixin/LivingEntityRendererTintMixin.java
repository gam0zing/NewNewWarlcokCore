package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.client.ModelColorSkinTint;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 染色皮肤使用半透明渲染类型，使纹理 alpha 参与混合。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererTintMixin {

    @Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
    private void newnew$translucentTintedAvatar(
        LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean outline,
        CallbackInfoReturnable<RenderType> callback) {
        if (!bodyVisible || outline) {
            return;
        }
        if (state instanceof AvatarRenderState avatar && ModelColorSkinTint.isTinted(avatar.skin)) {
            callback.setReturnValue(RenderTypes.entityTranslucentCull(avatar.skin.body().texturePath()));
        }
    }
}
