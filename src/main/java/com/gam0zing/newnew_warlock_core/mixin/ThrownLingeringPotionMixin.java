package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 滞留型药水的范围倍率。
 * <p>原版药水云初始半径写死为 3，这里乘 {@code potion_range}；
 * 后续的收缩速率（{@code setRadiusPerTick}）按新半径与时长自动计算，无需再改。</p>
 */
@Mixin(ThrownLingeringPotion.class)
public abstract class ThrownLingeringPotionMixin {

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(floatValue = 3.0F))
    private float newnew$scaleLingeringRadius(float original) {
        return original * this.newnew$potionRange();
    }

    @Unique
    private float newnew$potionRange() {
        Entity owner = ((ThrownLingeringPotion) (Object) this).getOwner();
        return owner instanceof LivingEntity living
            ? ModAttributes.getScale(living, ModAttributes.POTION_RANGE)
            : 1.0F;
    }
}
