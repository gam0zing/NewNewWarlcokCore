package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * 箭矢基础伤害的访问器。
 * <p>原版 {@link AbstractArrow} 只提供 {@code setBaseDamage}，没有读取方法，
 * 而幻影箭矢需要按比例削减已有基础伤害，因此在这里暴露 {@code baseDamage}。</p>
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Accessor("baseDamage")
    double newnew$getBaseDamage();

    @Accessor("baseDamage")
    void newnew$setBaseDamage(double baseDamage);
}
