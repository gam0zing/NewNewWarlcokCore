package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 自动扳机。
 * <p>玩家持有 {@code auto_fire > 0} 时，拉满的弓或上弦完成的弩会在服务端自动松开使用键：
 * 弓直接发射，弩完成上弦；服务端停止使用后，客户端仍按住的右键会触发原版的自动再次使用
 * （{@code Minecraft#handleKeybinds} 中 {@code !player.isUsingItem()} 分支），
 * 于是下一次使用紧接着开始，形成持续自动射击。</p>
 * <p>因此这里不需要维护任何逐玩家状态，也不需要在服务端手动重开使用：使用状态由原版
 * 实体数据标志同步，自动续接由客户端原版逻辑完成。只在服务端结算。</p>
 * <p>弩的蓄力时长由 {@link CrossbowItem#getChargeDuration(ItemStack, net.minecraft.world.entity.LivingEntity)}
 * 读取，已包含快速装填等附魔的修正，无需为附魔单独判断。</p>
 */
@Mixin(Player.class)
public abstract class PlayerAutoFireMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void newnew$autoFire(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || !self.isUsingItem()) {
            return;
        }
        if (ModAttributes.getValue(self, ModAttributes.AUTO_FIRE) <= 0.0D) {
            return;
        }

        ItemStack using = self.getUseItem();
        int ticks = self.getTicksUsingItem();

        if (using.getItem() instanceof BowItem) {
            if (BowItem.getPowerForTime(ticks) >= 1.0F) {
                self.releaseUsingItem();
            }
        } else if (using.getItem() instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(using)
                && ticks >= CrossbowItem.getChargeDuration(using, self)) {
                self.releaseUsingItem();
            }
        }
    }
}
