package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mixin.MobEffectInstanceStackAccess;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 史莱姆的环境状态（冰冻 / 失水）的公共逻辑。
 * <p>施加时 1 级、持续 2 分钟；到期前按宿主所在环境重新结算：对应温度升 1 级并续期，
 * 合适温度降 1 级，对立温度降 2 级，降到 0 级即移除。</p>
 * <p>周期伤害走 {@code inner_damage} 标签，由伤害管线路由为内部伤害。</p>
 */
public abstract class SlimeClimateMobEffect extends MobEffect {

    protected SlimeClimateMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    /** {@return 本效果注册项} */
    protected abstract Holder<MobEffect> self();

    /** {@return 对本效果而言的「对应温度」} */
    protected abstract BiomeClimate matchingClimate();

    /** {@return 该等级的速度/跳跃削减比例，可能大于 1（由使用方截断）} */
    public abstract float reduction(int level);

    /** {@return 该等级每 5 秒的内部伤害，0 表示不结算} */
    protected abstract float periodicDamage(int level);

    /** {@return 周期伤害使用的伤害类型} */
    protected abstract ResourceKey<DamageType> damageType();

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1 || duration % OriginTuning.SLIME_CLIMATE_DAMAGE_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        MobEffectInstance instance = entity.getEffect(self());
        if (instance == null || instance.getDuration() > 1) {
            float damage = periodicDamage(amplifier + 1);
            if (damage > 0.0F) {
                hurt(entity, damage);
            }
            return true;
        }
        int next = amplifier + 1 + levelDelta(BiomeClimate.at(entity));
        if (next <= 0) {
            return false;
        }
        MobEffectInstanceStackAccess access = (MobEffectInstanceStackAccess) instance;
        access.newnew$setAmplifier(next - 1);
        access.newnew$setDuration(durationFor(entity));
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), instance, false));
        }
        return true;
    }

    /**
     * {@return 目标当前状态对应的每轮持续时长}
     * <p>分裂 II 30 秒、分裂 I 60 秒、巨大化 4 分钟，其余 2 分钟；分裂优先于巨大化。</p>
     */
    public static int durationFor(LivingEntity entity) {
        MobEffectInstance split = entity.getEffect(ModMobEffects.SLIME_SPLIT);
        if (split != null) {
            return split.getAmplifier() > 0
                ? OriginTuning.SLIME_CLIMATE_DURATION_SPLIT_TWO_TICKS
                : OriginTuning.SLIME_CLIMATE_DURATION_SPLIT_ONE_TICKS;
        }
        if (entity.getEffect(ModMobEffects.SLIME_GIANT) != null) {
            return OriginTuning.SLIME_CLIMATE_DURATION_GIANT_TICKS;
        }
        return OriginTuning.SLIME_CLIMATE_DURATION_TICKS;
    }

    private int levelDelta(BiomeClimate climate) {
        if (climate == matchingClimate()) {
            return 1;
        }
        return climate == BiomeClimate.NORMAL ? -1 : -2;
    }

    private void hurt(LivingEntity entity, float amount) {
        Holder<DamageType> type = entity.damageSources().damageTypes.get(damageType()).orElse(null);
        if (type == null) {
            NewNewWarlockCore.LOGGER.warn("[slime_climate] 伤害类型 {} 未加载，本次伤害未结算",
                damageType().identifier());
            return;
        }
        entity.hurt(new DamageSource(type), amount);
    }

    /** 统一的伤害类型键构造。 */
    protected static ResourceKey<DamageType> damageTypeKey(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path));
    }
}
