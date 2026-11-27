package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;

/**
 * 石巨人：白天、晴天、露天时每秒恢复 1 点生命值。
 */
final class SunlightPower extends TraitPower {

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        var level = player.level();
        if (!level.isBrightOutside() || level.isRaining() || level.isThundering()
            || !level.canSeeSky(player.blockPosition())) {
            return;
        }
        player.heal(1.0F);
    }
}
