package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 喷溅型药水的范围倍率。
 * <p>原版影响半径写死为 4（平方 16），这里把三个半径常量整体乘 {@code potion_range}：
 * 4 → 4×scale，16 → 16×scale²，纵向的 2 → 2×scale。</p>
 */
@Mixin(ThrownSplashPotion.class)
public abstract class ThrownSplashPotionMixin {

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 4.0))
    private double newnew$scaleSplashRadius(double original) {
        return original * this.newnew$potionRange();
    }

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 2.0))
    private double newnew$scaleSplashRadiusY(double original) {
        return original * this.newnew$potionRange();
    }

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 16.0))
    private double newnew$scaleSplashRadiusSq(double original) {
        double scale = this.newnew$potionRange();
        return original * scale * scale;
    }

    @Unique
    private double newnew$potionRange() {
        Entity owner = ((ThrownSplashPotion) (Object) this).getOwner();
        return owner instanceof LivingEntity living
            ? ModAttributes.getScale(living, ModAttributes.POTION_RANGE)
            : 1.0D;
    }
}
