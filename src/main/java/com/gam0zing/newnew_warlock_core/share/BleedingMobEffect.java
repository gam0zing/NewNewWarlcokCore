package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 出血：每 {@link #INTERVAL_TICKS} 游戏刻造成一次内部伤害。
 * <p>伤害 = max(目标当前生命值 × {@link #HEALTH_RATIO_PER_LEVEL} × 效果等级,
 * {@link #MIN_DAMAGE_PER_LEVEL} × 效果等级)；
 * 等级按原版语义计算（{@code amplifier} 0 表示 I 级，即 1）。</p>
 * <p>伤害类型为 {@code newnew_warlock_core:bleeding}，属于 {@code inner_damage} 标签：
 * 伤害走正常的 {@link LivingEntity#hurt} 入口，由伤害管线按标签路由到内部伤害结算，
 * 不经过任何防护与加成流程。</p>
 */
public class BleedingMobEffect extends MobEffect {

    /** 结算间隔：每 100 游戏刻一次 */
    public static final int INTERVAL_TICKS = 100;
    /** 每级造成的当前生命值比例 */
    public static final float HEALTH_RATIO_PER_LEVEL = 0.05F;
    /** 每级的最低伤害 */
    public static final float MIN_DAMAGE_PER_LEVEL = 0.2F;

    private static final ResourceKey<DamageType> DAMAGE_TYPE =
        ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "bleeding"));

    public BleedingMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Holder<DamageType> type = entity.damageSources().damageTypes
            .get(DAMAGE_TYPE)
            .orElse(null);
        if (type == null) {
            NewNewWarlockCore.LOGGER.warn("[bleeding] 伤害类型 {} 未加载，本次出血未结算", DAMAGE_TYPE.identifier());
            return true;
        }

        int effectLevel = currentLevel(amplifier);
        float damage = Math.max(
            entity.getHealth() * HEALTH_RATIO_PER_LEVEL * effectLevel,
            MIN_DAMAGE_PER_LEVEL * effectLevel);
        entity.hurt(new DamageSource(type), damage);
        return true;
    }

    /**
     * {@return 本次结算使用的等级}
     * <p>容器模式下读取全部子实例的等级合计值，不受 {@code amplifier} 的 255 上限限制；
     * 普通实例仍按原版语义 {@code amplifier + 1} 计算。</p>
     */
    private static int currentLevel(int amplifier) {
        if (EffectSources.currentTickingEffect() instanceof EffectStack stack) {
            return Math.max(1, stack.totalLevel());
        }
        return amplifier + 1;
    }
}
