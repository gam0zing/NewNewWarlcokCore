package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.effect.MobEffectInstance;

/**
 * 给容器暴露递减子实例时长的能力。
 * <p>原版 {@code duration} 为 private 且没有 setter；容器逐 tick 结算子实例时需要写回剩余时长。
 * 容器的聚合展示值全部走重写的 getter，不再需要写回其它字段。</p>
 */
@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceStackAccess {

    @Accessor("duration")
    void newnew$setDuration(int duration);
}
