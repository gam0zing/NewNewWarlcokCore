package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.mob.ExplorerBonus;

/**
 * 探险家的移动速度加成（生物侧）。
 * <p>在 {@code LivingEntity#getSpeed()} 的出口乘上 {@code 属性值 × 当前位置难度}% 的加成，
 * 属性面板保持原样。玩家重写了 {@code getSpeed()} 不走这里，玩家侧见
 * {@code PlayerRunSpeedMixin}。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityExplorerMixin {

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void newnew$explorerSpeed(CallbackInfoReturnable<Float> cir) {
        double multiplier = ExplorerBonus.speedMultiplier((LivingEntity) (Object) this);
        if (multiplier != 1.0D) {
            cir.setReturnValue((float) (cir.getReturnValue() * multiplier));
        }
    }
}
