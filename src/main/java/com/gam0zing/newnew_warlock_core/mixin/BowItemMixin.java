package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 蓄力速度（弓）。
 * <p>原版在 {@code BowItem#releaseUsing} 里用「拉弓时长」计算箭矢威力，时长先交给
 * {@link EventHooks#onArrowLoose(ItemStack, Level, Player, int)} 供事件修正。
 * 这里在该事件之后把时长乘以 {@code charge_speed}，使更短的现实时间即可拉满。</p>
 * <p>弓的拉动进度（第一人称/第三人称表现与自动扳机判定）由
 * {@code LivingEntity#getTicksUsingItem()} 提供，已在 {@code LivingEntityChargeSpeedMixin} 里同步缩放，
 * 因此表现与实际威力一致。</p>
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {

    @Redirect(
        method = "releaseUsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/EventHooks;onArrowLoose(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;I)I"))
    private int newnew$scaleChargeTime(ItemStack weapon, Level level, Player player, int charge) {
        int adjusted = EventHooks.onArrowLoose(weapon, level, player, charge);
        if (adjusted < 0) {
            return adjusted;
        }

        float scale = ModAttributes.getScale(player, ModAttributes.CHARGE_SPEED);
        if (scale == 1.0F) {
            return adjusted;
        }

        return (int) Math.min(72000.0D, Math.round(adjusted * (double) scale));
    }
}
