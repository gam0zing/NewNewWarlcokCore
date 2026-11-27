package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 种族跳跃规则：石巨人无法跳跃。
 * <p>{@code jumpFromGround} 定义在 {@link LivingEntity} 上，由玩家实例进入分支。
 * 猫的蹬墙跳不在着地路径上，由 {@code MovementPower} 读取按键处理。</p>
 */
@Mixin(LivingEntity.class)
public abstract class OriginJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void newnew$originJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        if (OriginTraits.blocksJump(player)) {
            ci.cancel();
        }
    }
}
