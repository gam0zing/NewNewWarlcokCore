package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;

/**
 * 猫的【短跑健将】：疾跑位移结算时额外消耗饥饿值。
 * <p>原版在该方法里对游泳、疾跑、潜行、行走分别调用 {@code causeFoodExhaustion}，
 * 其中只有疾跑会传入大于 0 的数值（潜行与行走传 0），因此按「正值 + 疾跑 + 在地面」筛选即可精确命中。</p>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSprintExhaustionMixin {

    @WrapOperation(
        method = "checkMovementStatistics",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void newnew$extraSprintExhaustion(ServerPlayer player, float amount, Operation<Void> original) {
        if (amount > 0.0F && player.isSprinting() && player.onGround() && !player.isInWater()
            && OriginTraits.hasPower(player, OriginPowers.CAT_SPRINT)) {
            amount *= (float) (1.0D + OriginTuning.CAT_SPRINT_EXHAUSTION_EXTRA);
        }
        original.call(player, amount);
    }
}
