package com.gam0zing.newnew_warlock_core.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 暴露 {@link LivingEntity} 的受保护效果钩子，供容器在服务端主动刷新属性与同步包。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityEffectHooks {

    @Invoker("onEffectAdded")
    void newnew$onEffectAdded(MobEffectInstance instance, @Nullable Entity entity);

    @Invoker("onEffectUpdated")
    void newnew$onEffectUpdated(MobEffectInstance instance, boolean forced, @Nullable Entity entity);
}
