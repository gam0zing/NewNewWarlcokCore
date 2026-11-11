package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 隐蔽：玩家被生物发现的范围按 {@code (1 - 隐蔽)} 缩放。
 * <p>挂在 {@link LivingEntity#getVisibilityPercent(ServerLevel, Entity)} 的出口：
 * 原版在该方法内已经结算完隐身、潜行、护甲覆盖与 NeoForge 可见性钩子，
 * 这里只对玩家再乘一次隐蔽系数，因此「被发现范围 = 原本值 × (1 - 隐蔽)」。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityConcealmentMixin {

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void newnew$applyConcealment(
        ServerLevel level, Entity targetingEntity, CallbackInfoReturnable<Double> cir) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        double concealment = ModAttributes.getValue(player, ModAttributes.CONCEALMENT);
        if (concealment == 0.0D) {
            return;
        }

        cir.setReturnValue(cir.getReturnValueD() * (1.0D - concealment));
    }
}
