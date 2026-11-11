package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 弹射物速度倍率。
 * <p>{@code Projectile#shoot(double, double, double, float, float)} 是所有发射路径的汇聚点
 * （{@code shootFromRotation} 与各类 {@code spawnProjectileUsingShoot} 最终都调用它）。
 * 26.3 中发射速度先在 {@code getMovementToShoot} 里折成速度矢量再交给 {@code setDeltaMovement}，
 * 因此这里缩放置入的速度矢量：等比缩放不改变方向，后续朝向计算不受影响；
 * {@code shootFromRotation} 叠加的发射者自身动量在缩放之后加入，也不受倍率影响。</p>
 * <p>生物发射流程中 owner 在 shoot 之前已经设置；owner 缺失或不是生物时按基础值 1.0 处理。</p>
 */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @ModifyArg(
        method = "shoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 newnew$scaleProjectileVelocity(Vec3 movement) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        if (owner instanceof LivingEntity living) {
            return movement.scale(ModAttributes.getScale(living, ModAttributes.PROJECTILE_VELOCITY));
        }
        return movement;
    }
}
