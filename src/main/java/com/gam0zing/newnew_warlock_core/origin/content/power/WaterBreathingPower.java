package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;

/**
 * 水下呼吸：不会溺水。
 * <p>每 tick 把氧气值补满，因此水下停留再久也不会开始窒息。</p>
 */
final class WaterBreathingPower extends TraitPower {

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        if (player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(player.getMaxAirSupply());
        }
    }
}
