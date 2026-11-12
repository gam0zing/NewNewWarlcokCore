package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;

import com.gam0zing.newnew_warlock_core.enchant.EnchantUnlockData;

/**
 * 记录首次打赢袭击。
 * <p>原版 {@code Raid#tick} 在胜利条件达成时把 {@code status} 设为 {@code VICTORY}，这里在方法返回后检查，
 * 胜利状态会持续到袭击对象被移除，因此会重复命中；{@link EnchantUnlockData#markRaidDefeated()} 内部有
 * 已标记短路，不会反复写盘。</p>
 */
@Mixin(Raid.class)
public abstract class RaidVictoryMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void warlock$recordVictory(ServerLevel level, CallbackInfo ci) {
        if (((Raid) (Object) this).isVictory()) {
            EnchantUnlockData.get(level).markRaidDefeated();
        }
    }
}
