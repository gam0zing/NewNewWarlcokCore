package com.gam0zing.newnew_warlock_core.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.neoforge.common.CommonHooks;

import com.gam0zing.newnew_warlock_core.effect.MobEffectStackHooks;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;

/**
 * 让末影龙不再免疫「出血」与「大出血」。
 * <p>原版 {@link EnderDragon} 直接覆写 {@code addEffect} 返回 {@code false}，
 * 连 {@code canAffected} 都不经过，因此只能在覆写方法上定向放行这两种效果：</p>
 * <ul>
 *     <li>出血：交给 {@link MobEffectStackHooks#tryApply}，与其它生物一样并入堆叠容器；</li>
 *     <li>大出血：{@code forceAddEffect} 写入后补一次 {@code onEffectStarted}，
 *     因为 {@code forceAddEffect} 不会触发瞬时效果的回调（原版 {@code addEffect} 会）。</li>
 * </ul>
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonBleedingMixin {

    @Inject(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void newnew$allowBleedingEffects(MobEffectInstance instance, @Nullable Entity source,
        CallbackInfoReturnable<Boolean> cir) {
        Holder<MobEffect> effect = instance.getEffect();
        if (effect.value() != ModMobEffects.BLEEDING.get() && effect.value() != ModMobEffects.HEMORRHAGE.get()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }

        Boolean handled = MobEffectStackHooks.tryApply(self, instance, source);
        if (handled != null) {
            cir.setReturnValue(handled);
            return;
        }

        if (!CommonHooks.canMobEffectBeApplied(self, instance, source)) {
            cir.setReturnValue(false);
            return;
        }
        self.forceAddEffect(instance, source);
        instance.onEffectStarted(self);
        cir.setReturnValue(true);
    }
}
