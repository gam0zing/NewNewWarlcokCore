package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.cyberday1.neoorigins.power.builtin.base.AbstractActivePower;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 史莱姆：主动分裂。
 * <p>执行一次与致命分裂相同的分裂（回满血量、叠加【分裂】、最大生命值下降），
 * 粘液球数量 = ⌊(分裂前当前生命值 - 分裂后最大生命值) / 5⌋，最小为 0；
 * 背包放不下的部分掉落在脚下。冷却 10 秒。</p>
 */
final class SlimeActiveSplitPower extends AbstractActivePower<SlimeActiveSplitPower.Config> {

    @Override
    public Codec<Config> codec() {
        return Config.CODEC;
    }

    @Override
    protected boolean execute(ServerPlayer player, Config config) {
        float lost = SlimeSplitPower.performSplit(player);
        if (lost < 0.0F) {
            return false;
        }
        int count = (int) Math.floor(lost / 5.0F);
        if (count > 0) {
            ItemStack balls = new ItemStack(Items.SLIME_BALL, count);
            player.getInventory().add(balls);
            if (!balls.isEmpty()) {
                player.drop(balls, false, Prediction.SERVER_ONLY);
            }
        }
        return true;
    }

    public record Config(int cooldownTicks) implements AbstractActivePower.Config {

        public static final Config INSTANCE = new Config(OriginTuning.SLIME_ACTIVE_SPLIT_COOLDOWN);
        public static final Codec<Config> CODEC = MapCodec.unitCodec(INSTANCE);

        @Override
        public String cooldownIcon() {
            return "minecraft:slime_ball";
        }
    }
}
