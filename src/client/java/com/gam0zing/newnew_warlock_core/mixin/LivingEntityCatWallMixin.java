package com.gam0zing.newnew_warlock_core.mixin;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 猫的贴墙滑落与蹬墙跳。
 * <p>玩家移动由客户端结算，服务端改速度不会生效，因此这里在生成本 tick 位移之前
 * 直接改写速度：攀附时清除水平速度（视角轻微偏转不会左右滑动）、按攀附强度抵消重力，
 * 并在跳跃键上升沿蹬墙。</p>
 * <p>服务端只按同样的几何条件扣饥饿值，见 {@code MovementPower#catWallCost}。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCatWallMixin {

    @Unique
    private boolean newnew$catWallLastJump;

    @Inject(
        method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            shift = At.Shift.BEFORE))
    private void newnew$catWallMove(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer player)) {
            return;
        }

        boolean jump = player.input.keyPresses.jump();
        boolean jumpPressed = jump && !this.newnew$catWallLastJump;
        this.newnew$catWallLastJump = jump;

        if (!OriginTraits.hasPower(player, OriginPowers.CAT_CLIMB)
            || player.isSpectator() || player.isDeadOrDying()
            || player.getAbilities().flying
            || player.isPassenger() || player.onClimbable()
            || player.isInWater() || player.isInLava() || player.isFallFlying()
            || player.onGround()) {
            return;
        }
        if (!player.input.hasForwardImpulse() || !OriginTraits.isWallAhead(player)) {
            return;
        }

        double factor = OriginTraits.catWallSlideFactor(player);
        if (factor >= 1.0D) {
            return;
        }

        Vec3 delta = player.getDeltaMovement();
        double y = delta.y < 0.0D ? delta.y * factor : delta.y;
        if (jumpPressed && player.getFoodData().getFoodLevel() > 6) {
            y = OriginTuning.CAT_WALL_JUMP_VELOCITY * (1.0D - factor);
        }
        player.setDeltaMovement(0.0D, y, 0.0D);
    }
}
