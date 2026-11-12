package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import com.gam0zing.newnew_warlock_core.enchant.EnchantEffectScaling;

/**
 * 深海探索者 3 级以上的水下移动收益。
 * <p>{@code LivingEntity#travelInWater} 用 {@code water_movement_efficiency} 属性插值水中加速度与阻力，
 * 属性被 {@link RangedAttributeWaterMovementMixin} 放宽到 1 以上后，这里把超出部分换算成继续加速。</p>
 * <p>下沉速度只减免 {@code getFluidFallingAdjustedMovement} 里的被动重力项，
 * 主动向下游动不受影响，避免高等级时完全无法下潜。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWaterEnchantMixin {

    @WrapOperation(
        method = "travelInWater",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double newnew$scaleWaterMovement(
        LivingEntity entity, Holder<Attribute> attribute, Operation<Double> original) {
        double raw = original.call(entity, attribute);
        if (attribute != Attributes.WATER_MOVEMENT_EFFICIENCY) {
            return raw;
        }
        return EnchantEffectScaling.waterMovementEfficiency(raw);
    }

    @WrapOperation(
        method = "travelInWater",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getFluidFallingAdjustedMovement"
                + "(DZLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 newnew$reduceWaterSinking(
        LivingEntity entity, double baseGravity, boolean isFalling, Vec3 movement, Operation<Vec3> original) {
        Vec3 result = original.call(entity, baseGravity, isFalling, movement);
        if (result.y >= 0.0D || baseGravity == 0.0D || entity.isSprinting()) {
            return result;
        }

        double scale = EnchantEffectScaling.waterSinkScale(
            entity.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY));
        if (scale >= 1.0D) {
            return result;
        }

        // 把被动重力项按比例还回去，并保证不会因此产生向上漂移。
        double reducedSink = (baseGravity / 16.0D) * (1.0D - scale);
        return new Vec3(result.x, Math.min(0.0D, result.y + reducedSink), result.z);
    }
}
