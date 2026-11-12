package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import com.gam0zing.newnew_warlock_core.enchant.EnchantEffectScaling;

/**
 * 解除 {@code water_movement_efficiency} 的 0~1 硬上限。
 * <p>原版深海探索者每级 +1/3，属性区间又限制在 0~1，所以 3 级就封顶。
 * 属性值超过 1 之后如何继续加速由 {@link EnchantEffectScaling#waterMovementEfficiency} 负责，
 * 这里只负责让属性本身能存下超过 1 的值。</p>
 * <p>只对这一个属性放宽，其它 {@link RangedAttribute} 保持原版区间。</p>
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeWaterMovementMixin {

    @Inject(method = "sanitizeValue", at = @At("RETURN"), cancellable = true)
    private void newnew$extendWaterMovementCap(double value, CallbackInfoReturnable<Double> cir) {
        RangedAttribute self = (RangedAttribute) (Object) this;
        if (self != Attributes.WATER_MOVEMENT_EFFICIENCY.value()) {
            return;
        }

        cir.setReturnValue(Mth.clamp(cir.getReturnValue(), self.getMinValue(),
            EnchantEffectScaling.WATER_EFFICIENCY_CAP));
    }
}
