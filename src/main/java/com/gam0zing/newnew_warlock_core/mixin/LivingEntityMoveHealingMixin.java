package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.combat.spine.WarlockHealingHost;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 移动治疗：每 tick 结算一次，按本 tick 的移动距离对自己调用 {@code healByLiving}。
 * <p>每累积移动满 1 格触发一次治疗，单次治疗量为属性值；一 tick 内移动多格时按格数合并成一次调用。
 * 累积距离每 tick 封顶 {@link #NEWNEW$MAX_ACCUMULATED} 格，防止传送、鞘翅冲刺等瞬移造成爆发治疗。</p>
 * <p>移动距离用自行记录的上一 tick 位置差分（{@code xo/zo} 在 {@code LivingEntity#tick} 尾部已被更新，
 * 无法用于本 tick 位移）；只在服务端结算。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMoveHealingMixin {

    /** 每 tick 计入结算的最大移动距离（格） */
    @Unique
    private static final double NEWNEW$MAX_ACCUMULATED = 3.0D;

    @Unique
    private double newnew$lastX = Double.NaN;

    @Unique
    private double newnew$lastY;

    @Unique
    private double newnew$lastZ;

    @Unique
    private double newnew$accumulated;

    @Inject(method = "tick", at = @At("TAIL"))
    private void newnew$moveHealing(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }

        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();
        if (Double.isNaN(this.newnew$lastX)) {
            this.newnew$lastX = x;
            this.newnew$lastY = y;
            this.newnew$lastZ = z;
            return;
        }

        double dx = x - this.newnew$lastX;
        double dy = y - this.newnew$lastY;
        double dz = z - this.newnew$lastZ;
        this.newnew$lastX = x;
        this.newnew$lastY = y;
        this.newnew$lastZ = z;

        double value = ModAttributes.getValue(self, ModAttributes.MOVE_HEALING);
        if (value <= 0.0D) {
            return;
        }

        this.newnew$accumulated = Math.min(
            this.newnew$accumulated + Math.sqrt(dx * dx + dy * dy + dz * dz), NEWNEW$MAX_ACCUMULATED);
        if (this.newnew$accumulated < 1.0D) {
            return;
        }

        int heals = (int) this.newnew$accumulated;
        this.newnew$accumulated -= heals;
        ((WarlockHealingHost) self).healByLiving(self, (float) (value * heals));
    }
}
