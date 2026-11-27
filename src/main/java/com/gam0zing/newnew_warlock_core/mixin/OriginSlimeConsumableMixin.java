package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 史莱姆可以把粘液球/粘液块当作可长按使用的消耗品。
 * <p>原版这两个物品没有 {@code CONSUMABLE} 组件，因此这里补齐完整的使用链路：
 * 开始使用、时长、进食动画、完成时结算并扣除 1 个。</p>
 */
@Mixin(Item.class)
public abstract class OriginSlimeConsumableMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void newnew$startUsing(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir) {
        Item item = (Item) (Object) this;
        if (OriginTraits.isSlimeConsumable(item, player)) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void newnew$useDuration(
        ItemStack stack,
        LivingEntity entity,
        CallbackInfoReturnable<Integer> cir) {
        int duration = OriginTraits.slimeUseDuration(stack, entity, cir.getReturnValueI());
        if (duration >= 0) {
            cir.setReturnValue(duration);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("RETURN"), cancellable = true)
    private void newnew$useAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (stack.is(Items.SLIME_BALL) || stack.is(Items.SLIME_BLOCK)) {
            cir.setReturnValue(ItemUseAnimation.EAT);
        }
    }

    /**
     * 进食过程中的咀嚼音效。
     * <p>原版只有在物品带 {@code CONSUMABLE} 组件时才会发声音，粘液球/粘液块没有该组件，
     * 因此这里按原版节流规则（跳过起始 21.875%，其余每 4 tick 一次）手动补上。</p>
     * <p>只在服务端播放，由服务端广播，客户端不会重复。</p>
     */
    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void newnew$chewSound(
        Level level,
        LivingEntity entity,
        ItemStack stack,
        int remaining,
        CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int total = OriginTraits.slimeConsumableDuration(stack, player);
        if (total <= 0) {
            return;
        }
        int used = total - remaining;
        if (level.isClientSide()) {
            return;
        }
        if (!OriginTraits.isSlimeConsumable((Item) (Object) this, player)) {
            return;
        }
        if (used < OriginTuning.SLIME_CHEW_START_TICKS || remaining % 4 != 0) {
            return;
        }
        // 音量与音高随机化，和原版进食一致。
        // 注意不能用 entity.playSound：Player 的实现会把玩家自己当作例外，自己听不到。
        level.playSound(
            null,
            entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.GENERIC_EAT,
            SoundSource.PLAYERS,
            player.getRandom().nextBoolean() ? 0.5F : 1.0F,
            player.getRandom().triangle(1.0F, 0.2F));
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void newnew$finishUsing(
        ItemStack stack,
        Level level,
        LivingEntity entity,
        CallbackInfoReturnable<ItemStack> cir) {
        if (entity instanceof Player player
            && OriginTraits.isSlimeConsumable((Item) (Object) this, player)) {
            cir.setReturnValue(OriginTraits.finishSlimeConsumable(player, stack));
        }
    }
}
