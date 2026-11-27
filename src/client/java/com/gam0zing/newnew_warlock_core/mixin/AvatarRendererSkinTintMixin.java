package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gam0zing.newnew_warlock_core.client.ModelColorSkinTint;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;

/**
 * 在渲染状态提取完成后套用染色，覆盖 morph 皮肤等其它来源。
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererSkinTintMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"))
    private void newnew$applyModelColor(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo callback) {
        if (state.skin == null) {
            return;
        }
        try {
            state.skin = ModelColorSkinTint.apply(avatar.getUUID(), state.skin);
        } catch (RuntimeException ignored) {
            // 渲染路径不应因染色失败而中断。
        }
    }
}
