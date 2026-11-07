package com.gam0zing.newnew_warlock_core.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.combat.effect.MobEffectStackHooks;

/**
 * 状态效果容器的原版施加入口适配。
 * <p>只负责入口分流：受容器管理的效果在 HEAD 接管，其余效果返回 null 放行原版。</p>
 * <p>容器本身就是该效果的子实例类型，直接存放在 {@code activeEffects} 中，
 * 因此 {@code getEffect} / 迭代 / 网络包 / 存档都无需额外适配。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectStackMixin {

    /**
     * 服务端施加入口：受容器管理的效果在 HEAD 接管，其余效果返回 null 放行原版。
     */
    @Inject(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void newnew$applyEffectStack(
        MobEffectInstance instance, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        Boolean result = MobEffectStackHooks.tryApply((LivingEntity) (Object) this, instance, source);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
