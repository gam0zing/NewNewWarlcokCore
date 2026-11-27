package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 种族疾跑规则：天堂鸟与巨大化史莱姆无法疾跑，猫无法疾泳。
 * <p>{@code setSprinting} 定义在 {@link LivingEntity} 上，由玩家实例进入分支。</p>
 * <p>客户端必须同样拦截：本地玩家会自行设置疾跑状态，服务端拒绝后不会主动下发纠正。</p>
 */
@Mixin(LivingEntity.class)
public abstract class OriginSprintMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void newnew$blockSprint(boolean sprinting, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!sprinting || !(self instanceof Player player)) {
            return;
        }
        if (OriginTraits.blocksSprint(player)
            || (OriginTraits.hasPower(player, OriginPowers.CAT_AQUAPHOBIC) && player.isInWater())) {
            ci.cancel();
        }
    }
}
