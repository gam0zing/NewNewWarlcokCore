package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.cyberday1.neoorigins.power.builtin.base.AbstractActivePower;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 天堂鸟振翅：朝视线方向冲刺，水下失效。
 * <p>复用 NeoOrigins 的主动能力按键与冷却分发；{@code execute} 返回 false 时不进入冷却。</p>
 */
public final class BirdDashPower extends AbstractActivePower<BirdDashPower.Config> {

    @Override
    public Codec<Config> codec() {
        return Config.CODEC;
    }

    @Override
    protected boolean execute(ServerPlayer player, Config config) {
        if (player.isUnderWater()) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(config.power()));
        player.syncVelocity = true;
        return true;
    }

    public record Config(float power, int cooldownTicks) implements AbstractActivePower.Config {

        public static final Config INSTANCE =
            new Config((float) OriginTuning.BIRD_DASH_POWER, OriginTuning.BIRD_DASH_COOLDOWN);
        public static final Codec<Config> CODEC = MapCodec.unitCodec(INSTANCE);

        @Override
        public int hungerCost() {
            return 1;
        }

        @Override
        public String cooldownIcon() {
            return "minecraft:feather";
        }
    }
}
