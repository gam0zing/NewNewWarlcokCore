package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;

import com.gam0zing.newnew_warlock_core.mob.WorldProgressData;

/**
 * 末地门成形标记。
 * <p>26.3 里向框架塞入末影之眼的逻辑在 {@link EnderEyeItem#useOn(UseOnContext)}：
 * 框架图案匹配成功后填充末地门方块，并以 1038 号全局事件播放激活音效。
 * NeoForge 的 {@code BlockEvent.PortalSpawnEvent} 只覆盖下界门，末地门成形没有
 * 现成事件，因此在播放 1038 号事件的调用点注入标记。</p>
 */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {

    @Inject(
        method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)"
            + "Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;globalLevelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void newnew$markEndOpened(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel() instanceof ServerLevel level) {
            WorldProgressData.get(level).markEndOpened();
        }
    }
}
