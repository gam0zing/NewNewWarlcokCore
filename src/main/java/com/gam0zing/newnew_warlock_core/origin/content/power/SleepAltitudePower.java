package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;

/**
 * 天堂鸟：高度不足时无法入睡。
 */
final class SleepAltitudePower extends TraitPower {

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        if (!player.isSleeping() || player.getY() >= OriginTuning.BIRD_MIN_SLEEP_Y) {
            return;
        }
        player.stopSleeping();
    }
}
