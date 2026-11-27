package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

/**
 * 史莱姆进食/饮用时的咀嚼音效时机。
 * <p>原版按「组件的进食时长 − 剩余时长」计算已用时长，但史莱姆的实际使用时长被体型改写过，
 * 两者不一致会导致音效在临近吃完时才响（慢速）或一开始就连响（快速）。
 * 这里改为按真实使用时长计算，并统一在与咀嚼动画相同的时刻开始。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackConsumeTimingMixin {

    @Redirect(
        method = "onUseTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/Consumable;shouldEmitParticlesAndSounds(I)Z"))
    private boolean newnew$retimeConsume(
        Consumable consumable,
        int remainingTicks,
        Level level,
        LivingEntity entity,
        int remaining) {
        if (entity instanceof Player player
            && OriginTraits.hasPower(player, OriginPowers.SLIME_LIFE)) {
            ItemStack self = (ItemStack) (Object) this;
            int total = self.getUseDuration(player);
            if (total != consumable.consumeTicks()) {
                int used = total - remaining;
                return used >= OriginTuning.SLIME_CHEW_START_TICKS && remaining % 4 == 0;
            }
        }
        return consumable.shouldEmitParticlesAndSounds(remainingTicks);
    }
}
