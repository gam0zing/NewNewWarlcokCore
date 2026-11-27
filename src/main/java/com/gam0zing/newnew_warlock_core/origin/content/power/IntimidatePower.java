package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.cyberday1.neoorigins.power.builtin.base.AbstractActivePower;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import com.gam0zing.newnew_warlock_core.combat.standard.VanillaDamageUtils;
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;
import com.gam0zing.newnew_warlock_core.share.UnstoppableMobEffect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;

/**
 * 猫：主动威慑周围生物。
 * <p>范围内的生物立刻丢失当前目标，并获得 3 秒【威慑】效果，期间无法锁定任何目标；冷却 10 秒。</p>
 */
final class IntimidatePower extends AbstractActivePower<IntimidatePower.Config> {

    @Override
    public Codec<Config> codec() {
        return Config.CODEC;
    }

    @Override
    protected boolean execute(ServerPlayer player, Config config) {
        ServerLevel level = player.level();
        double catVolume = Math.max(1.0E-4D, VanillaDamageUtils.boundingBoxVolume(player));
        boolean affected = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class,
            player.getBoundingBox().inflate(config.radius()))) {
            affected = true;
            mob.setTarget(null);
            mob.getNavigation().stop();
            // 目标体积越大，威慑持续越短：时长 = 基础时长 × 双方体积比
            double ratio = catVolume / Math.max(1.0E-4D, VanillaDamageUtils.boundingBoxVolume(mob));
            int duration = Math.max(1, (int) Math.round(OriginTuning.CAT_INTIMIDATE_DURATION * ratio));
            // 不可阻挡：每级等比例削减威慑时长
            MobEffectInstance unstoppable = mob.getEffect(ModMobEffects.UNSTOPPABLE);
            if (unstoppable != null) {
                double keep = 1.0D
                    - UnstoppableMobEffect.RESISTANCE_PER_LEVEL * (unstoppable.getAmplifier() + 1);
                if (keep <= 0.0D) {
                    continue;
                }
                duration = Math.max(1, (int) Math.round(duration * keep));
            }
            EffectSources.apply(mob, new MobEffectInstance(ModMobEffects.INTIMIDATED, duration, 0), player);
        }
        return affected;
    }

    public record Config(int cooldownTicks, double radius) implements AbstractActivePower.Config {

        public static final Config INSTANCE =
            new Config(OriginTuning.CAT_INTIMIDATE_COOLDOWN, OriginTuning.CAT_INTIMIDATE_RADIUS);
        public static final Codec<Config> CODEC = MapCodec.unitCodec(INSTANCE);

        @Override
        public String cooldownIcon() {
            return "minecraft:cat_spawn_egg";
        }
    }
}
