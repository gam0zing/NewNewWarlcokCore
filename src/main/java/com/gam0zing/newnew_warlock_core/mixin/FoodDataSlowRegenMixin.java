package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * 史莱姆「消化」：饱食度自然回血（饱食度 → 血量）的速度随体型变化。
 * <p>每次回血的治疗量不变，只改变触发频率：累计消化进度达到 1 才结算一次，
 * 并同步结算同等份数的饥饿消耗，因此效率不变，只是快慢不同。</p>
 */
@Mixin(FoodData.class)
public abstract class FoodDataSlowRegenMixin {

    @Unique
    private double newnew$digestCredit;

    @Unique
    private int newnew$regenTimes = 1;

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void newnew$digestNaturalRegen(ServerPlayer player, float amount) {
        int times = 1;
        if (OriginTraits.hasPower(player, OriginPowers.SLIME_LIFE)) {
            this.newnew$digestCredit += OriginTraits.slimeDigestRate(player);
            times = (int) Math.floor(this.newnew$digestCredit);
            this.newnew$digestCredit -= times;
        }
        this.newnew$regenTimes = times;
        for (int i = 0; i < times; i++) {
            player.heal(amount);
        }
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private void newnew$scaleRegenExhaustion(FoodData food, float amount) {
        int times = this.newnew$regenTimes;
        this.newnew$regenTimes = 1;
        if (times > 0) {
            food.addExhaustion(amount * times);
        }
    }
}
