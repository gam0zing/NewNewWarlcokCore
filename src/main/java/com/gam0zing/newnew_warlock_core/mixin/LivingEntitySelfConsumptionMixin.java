package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.share.EffectSources;

/**
 * 自己吃喝东西时的来源入口。
 * <p>{@code LivingEntity#completeUsingItem()} 覆盖玩家/生物自己完成使用物品（喝药、食物等），
 * 直接把“使用者本人”登记为来源；不覆盖喷溅/滞留/药箭等“施加给他人”的路径，
 * 它们由原版 {@code addEffect} 的实体参数提供投掷者来源。</p>
 * <p>26.3 移除 {@code LivingEntity#eat} 后，绕过 {@code completeUsingItem} 的直接结算路径
 * （如狐狸进食）由 {@link ItemStackFinishUsingMixin} 覆盖。</p>
 * <p>客户端也会登记，但只有服务端真正施加效果，客户端登记无副作用。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySelfConsumptionMixin {

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void newnew$beginSelfConsumption(CallbackInfo ci) {
        EffectSources.beginSelfConsumption((LivingEntity) (Object) this);
    }

    @Inject(method = "completeUsingItem", at = @At("RETURN"))
    private void newnew$endSelfConsumption(CallbackInfo ci) {
        EffectSources.endSelfConsumption();
    }
}
