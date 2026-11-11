package com.gam0zing.newnew_warlock_core.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import com.gam0zing.newnew_warlock_core.mob.config.MobDifficultyConfig;
import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;

/**
 * 生物重建的难度与 points 计算。
 * <p>数据流向：{@code points 基准 --> 难度增幅 --> 正态分布随机化 --> 四维 --> 属性与技能}。</p>
 * <p>难度来源（全部来自 {@link MobDifficultyConfig}）：</p>
 * <ul>
 *     <li>游戏天数（按主世界时钟的天数）</li>
 *     <li>相对海平面的垂直距离（每格 y 轴）</li>
 *     <li>相对世界出生点的水平距离（每 100 格直线距离）</li>
 *     <li>传送门进度（主世界专用）</li>
 *     <li>维度附加难度、群系附加难度</li>
 * </ul>
 * <p>服务器游戏难度（简单/普通/困难）作为总倍率，作用在该生物 points 计算的最后一步；
 * 和平固定为 1。随机化使用均值 = 难度增幅后的 points、标准差 = 均值的 15%，
 * 结果下限截断为 0。</p>
 */
public final class MobDifficulty {

    private static final double TICKS_PER_DAY = 24_000.0D;
    private static final double RANDOM_STDDEV_RATIO = 0.15D;

    private MobDifficulty() {}

    /** 汇总当前实体位置、时间与世界进度得到的难度值。 */
    public static double rawDifficulty(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel)) {
            return 0.0D;
        }
        return rawDifficultyAt(
            entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.blockPosition());
    }

    /**
     * 汇总指定位置、时间与世界进度得到的难度值。
     * <p>需要 {@link ServerLevel} 才能读到传送门进度（{@link WorldProgressData}）；
     * 其它层级上该因素按 0 处理。客户端显示走服务端同步，不调用本方法。</p>
     */
    public static double rawDifficultyAt(Level level, double x, double y, double z, BlockPos pos) {
        double difficulty = MobDifficultyConfig.dayBonus(level.getOverworldClockTime() / TICKS_PER_DAY);
        difficulty += MobDifficultyConfig.verticalBonus(y, level.getSeaLevel());

        BlockPos spawn = level.getRespawnData().pos();
        difficulty += MobDifficultyConfig.horizontalBonus(x - spawn.getX(), z - spawn.getZ());

        if (level.dimension().equals(Level.OVERWORLD) && level instanceof ServerLevel serverLevel) {
            WorldProgressData progress = WorldProgressData.get(serverLevel);
            difficulty += MobDifficultyConfig.portalBonus(progress.netherOpened(), progress.endOpened());
        }

        difficulty += MobDifficultyConfig.dimensionBonus(level.dimension().identifier());

        Identifier biome = level.getBiome(pos)
            .unwrapKey()
            .map(key -> key.identifier())
            .orElse(null);
        difficulty += MobDifficultyConfig.biomeBonus(biome);

        return difficulty;
    }

    /** points 基准值经难度增幅、并乘上服务器难度总倍率后的结果。 */
    public static double scaledPoints(LivingEntity entity, MobPointsConfig.Entry entry) {
        double points = entry.pointsBase() + rawDifficulty(entity) * entry.pointsGainPerDifficulty();
        double multiplier = MobDifficultyConfig.globalMultiplier(entity.level().getDifficulty());
        return Math.max(0.0D, points * multiplier);
    }

    /**
     * 险地类概率翻倍：按位置难度把概率值折算成翻倍次数。
     * <p>难度 &gt;= 30 时概率取全值；难度 15~30 时概率减半；低于 15 不生效。
     * 整数部分为必定翻倍的次数，小数部分为再翻一次的概率（可连续翻倍：2 变 4、4 变 8）。</p>
     *
     * @param value 属性给出的概率值（如险地矿工、探险家）
     * @return 翻倍次数；数量应乘以 2 的该次幂
     */
    public static int rollDoublingTimes(
        double value, Level level, double x, double y, double z, BlockPos pos, RandomSource random) {
        if (value <= 0.0D) {
            return 0;
        }
        double difficulty = rawDifficultyAt(level, x, y, z, pos);
        double chance = difficulty >= 30.0D ? value : difficulty >= 15.0D ? value * 0.5D : 0.0D;
        int times = (int) Math.floor(chance);
        if (random.nextDouble() < chance - times) {
            times++;
        }
        return times;
    }

    /** 正态分布随机化：均值 {@code mean}，标准差为均值的 15%，下限 0。 */
    public static double randomize(LivingEntity entity, double mean) {
        if (!(mean > 0.0D)) {
            return 0.0D;
        }
        double rolled = mean * (1.0D + entity.getRandom().nextGaussian() * RANDOM_STDDEV_RATIO);
        return Math.max(0.0D, rolled);
    }
}
