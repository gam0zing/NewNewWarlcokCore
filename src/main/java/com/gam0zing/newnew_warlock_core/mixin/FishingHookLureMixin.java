package com.gam0zing.newnew_warlock_core.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;

import com.gam0zing.newnew_warlock_core.enchant.EnchantEffectScaling;

/**
 * 让饵钓在 3 级以上继续生效。
 * <p>原版 {@code FishingHook#catchingFish} 的等待时间为
 * {@code rand(100, 600) - lureSpeed}，其中 {@code lureSpeed = 100 × 等级}。
 * 6 级及以上时该值恒不大于 0，会每 tick 重新掷骰并永远不咬钩。</p>
 * <p>这里在“掷出等待时间并减去 lureSpeed”之后重新计算一次，
 * 改为按等级比例递减并保底 1 秒，1~3 级的节奏与原版基本一致。</p>
 */
@Mixin(FishingHook.class)
public abstract class FishingHookLureMixin {

    @Shadow
    @Final
    private int lureSpeed;

    @Shadow
    private int timeUntilLured;

    /**
     * 注入点是方法里的第 4 次（ordinal = 3）对 {@code timeUntilLured} 的写入，
     * 即 {@code this.timeUntilLured = this.timeUntilLured - this.lureSpeed} 之后。
     * 此时字段值等于原始掷骰结果减去 lureSpeed，可以反向还原出原始掷骰结果。
     */
    @Inject(
        method = "catchingFish",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;timeUntilLured:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 3,
            shift = At.Shift.AFTER))
    private void newnew$scaleLureWait(BlockPos pos, CallbackInfo ci) {
        int baseWait = this.timeUntilLured + this.lureSpeed;
        this.timeUntilLured = EnchantEffectScaling.lureWaitTicks(baseWait, this.lureSpeed);
    }
}
