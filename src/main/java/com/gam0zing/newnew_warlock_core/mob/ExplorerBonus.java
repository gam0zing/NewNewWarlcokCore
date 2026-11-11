package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.world.entity.LivingEntity;

/**
 * 探险家属性的两条收益规则。
 * <ul>
 *     <li>吸收经验球时按险地矿工同款概率使经验数值翻倍（{@link MobDifficulty#rollDoublingTimes}）；</li>
 *     <li>移动速度提升 {@code 属性值 × 当前位置难度}%。</li>
 * </ul>
 */
public final class ExplorerBonus {

    /**
     * 经验球翻倍后的经验值。
     *
     * @param amount 经验球进入经验条的经验值（经验修补消耗后的剩余）
     * @return 翻倍后的经验值
     */
    public static int doubleOrbXp(LivingEntity absorber, int amount) {
        if (amount <= 0) {
            return amount;
        }
        double value = ModAttributes.getValue(absorber, ModAttributes.EXPLORER);
        int times = MobDifficulty.rollDoublingTimes(
            value,
            absorber.level(),
            absorber.getX(),
            absorber.getY(),
            absorber.getZ(),
            absorber.blockPosition(),
            absorber.getRandom());
        if (times <= 0) {
            return amount;
        }
        long scaled = (long) amount << Math.min(times, 30);
        return (int) Math.min(scaled, Integer.MAX_VALUE);
    }

    /** {@return 探险家提供的移动速度乘区倍率；属性为 0 时返回 1，此时不做难度计算} */
    public static double speedMultiplier(LivingEntity entity) {
        double explorer = ModAttributes.getValue(entity, ModAttributes.EXPLORER);
        if (explorer <= 0.0D) {
            return 1.0D;
        }
        double difficulty = MobDifficulty.rawDifficultyAt(
            entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.blockPosition());
        return 1.0D + explorer * difficulty / 100.0D;
    }

    private ExplorerBonus() {}
}
