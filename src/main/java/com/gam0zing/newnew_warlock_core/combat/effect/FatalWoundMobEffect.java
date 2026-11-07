package com.gam0zing.newnew_warlock_core.combat.effect;

import com.gam0zing.newnew_warlock_core.registry.ModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

/**
 * 致命伤：瞬时效果，施加时立即结算一次。
 * <p>按自身等级（{@code amplifier} 0 表示 I 级）随机施加：</p>
 * <ul>
 *     <li>出血：等级在 [0, 2×等级] 内随机，时长为 {@code 1200 + 600×等级} 游戏刻。</li>
 * </ul>
 * <p>随机到 0 级表示不施加该效果。</p>
 */
public class FatalWoundMobEffect extends InstantaneousMobEffect {

    /** 出血基础时长 */
    public static final int BLEEDING_BASE_DURATION = 1200;
    /** 出血每级追加时长 */
    public static final int BLEEDING_DURATION_PER_LEVEL = 600;

    public FatalWoundMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyInstantaneousEffect(
        ServerLevel level, @Nullable Entity source, @Nullable Entity indirectSource,
        LivingEntity target, int amplifier, double health) {
        this.applyPayload(target, amplifier);
    }

    @Override
    public void onEffectStarted(LivingEntity target, int amplifier) {
        // 常规 addEffect 路径不会调用 applyInstantenousEffect，这里补齐结算并立即移除自身
        this.applyPayload(target, amplifier);
        target.removeEffect(ModMobEffects.FATAL_WOUND);
    }

    private void applyPayload(LivingEntity target, int amplifier) {
        if (target.level().isClientSide() || target.isDeadOrDying()) {
            return;
        }

        int level = amplifier + 1;
        RandomSource random = target.getRandom();

        int bleedingLevel = random.nextInt(2 * level + 1);
        if (bleedingLevel > 0) {
            target.addEffect(new BleedingEffectInstance(
                ModMobEffects.BLEEDING,
                BLEEDING_BASE_DURATION + BLEEDING_DURATION_PER_LEVEL * level,
                bleedingLevel - 1));
        }
    }
}
