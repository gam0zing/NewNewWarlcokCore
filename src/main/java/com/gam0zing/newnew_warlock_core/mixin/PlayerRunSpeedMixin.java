package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.mob.ExplorerBonus;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 奔跑速度倍率与探险家的移动速度加成（玩家侧）。
 * <p>疾跑加成在原版是以 {@code MOVEMENT_SPEED} 的瞬时修饰符实现的，因此这里不改面板数值，
 * 只在 {@link Player#getSpeed()} 的出口乘 {@code run_speed}：移动计算读到的就是倍率后的速度，
 * 属性面板保持原样。探险家的 {@code 属性值 × 当前位置难度}% 移速加成在同一出口叠加。</p>
 * <p>玩家重写了 {@code LivingEntity#getSpeed()}，所以必须挂在玩家自己的实现上。</p>
 */
@Mixin(Player.class)
public abstract class PlayerRunSpeedMixin {

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void newnew$scaleRunSpeed(CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;

        float speed = cir.getReturnValue();
        if (self.isSprinting()) {
            speed *= ModAttributes.getScale(self, ModAttributes.RUN_SPEED);
        }

        double explorerMultiplier = ExplorerBonus.speedMultiplier(self);
        if (explorerMultiplier != 1.0D) {
            speed = (float) (speed * explorerMultiplier);
        }

        if (speed != cir.getReturnValue()) {
            cir.setReturnValue(speed);
        }
    }
}
