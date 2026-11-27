package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;

/**
 * 史莱姆进食的第一人称动画节奏修正。
 * <p>进食时长被体型拉长后，原版的「抬到嘴边」进度按 剩余时长/总时长 计算，导致抬手非常慢。
 * 这里改成固定按原版 32 tick 的节奏抬手，咀嚼抖动与咀嚼音效都在同一时刻开始。</p>
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonEatAnimationMixin {

    @Inject(method = "applyEatTransform", at = @At("HEAD"), cancellable = true)
    private void newnew$slimeEatTransform(
        PoseStack poseStack,
        float partialTicks,
        HumanoidArm arm,
        float remaining,
        int duration,
        CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null
            || !(OriginTraits.hasPower(player, OriginPowers.SLIME_LIFE)
                || OriginTraits.hasPower(player, OriginPowers.SLIME_DEVOUR))) {
            return;
        }
        ItemUseAnimation animation = player.getUseItem().getUseAnimation();
        if (animation != ItemUseAnimation.EAT && animation != ItemUseAnimation.DRINK) {
            return;
        }
        float used = Math.max(0.0F, (float) duration - remaining + partialTicks);
        float phase = remaining - partialTicks + 1.0F;
        if (used >= OriginTuning.SLIME_CHEW_START_TICKS) {
            poseStack.translate(0.0F, Math.abs(Mth.cos(phase / 4.0F * (float) Math.PI) * 0.1F), 0.0F);
        }
        // 抬升进度按原版进食时长计算，与本次进食的总时长无关
        float ratio = Mth.clamp(1.0F - used / (float) OriginTuning.VANILLA_EAT_DURATION_TICKS, 0.0F, 1.0F);
        float rise = 1.0F - (float) Math.pow(ratio, 27.0D);
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(rise * 0.6F * side, rise * -0.5F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, side * rise * 90.0F);
        poseStack.rotateDegrees(Axis.XP, rise * 10.0F);
        poseStack.rotateDegrees(Axis.ZP, side * rise * 30.0F);
        ci.cancel();
    }
}
