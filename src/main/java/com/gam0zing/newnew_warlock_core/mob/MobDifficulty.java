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
 * <p>服务器游戏难度（简单/普通/困难）是一个固定难度偏移（默认 -12 / 0 / +12），只影响怪物：
 * 怪物使用 {@link #scaledDifficulty(LivingEntity)}（原始难度 + 偏移）计算 points；
 * 玩家侧加成（探险家、险地矿工等）使用不含偏移的原始难度，保证阶段进度不随难度选择变化。
 * 和平固定为 0。随机化使用均值 = 难度增幅后的 points、标准差 = 均值的 15%，
 * 结果下限截断为 0。</p>
 */
public final class MobDifficulty {

    private static final double TICKS_PER_DAY = 24_000.0D;
    private static final double RANDOM_STDDEV_RATIO = 0.15D;

    private MobDifficulty() {}

    /** 汇总当前实体位置、时间与世界进度得到的原始难度（不含服务器游戏难度偏移）。 */
    public static double rawDifficulty(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel)) {
            return 0.0D;
        }
        return rawDifficultyAt(
            entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.blockPosition());
    }

    /**
     * 怪物使用的难度：原始难度再加上服务器游戏难度的固定偏移。
     * <p>只有怪物侧的数值（points）应该用它；玩家侧加成一律使用 {@link #rawDifficulty(LivingEntity)}。
     * 与非 ServerLevel 上的原始难度一致返回 0。</p>
     */
    public static double scaledDifficulty(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel)) {
            return 0.0D;
        }
        return rawDifficulty(entity)
            + MobDifficultyConfig.difficultyOffset(entity.level().getDifficulty());
    }

    /**
     * 汇总指定位置、时间与世界进度得到的原始难度（不含服务器难度偏移）。
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

    /** points 基准值加上经“难度 + 服务器难度偏移”增幅后的增量。 */
    public static double scaledPoints(LivingEntity entity, MobPointsConfig.Entry entry) {
        double points = entry.pointsBase() + scaledDifficulty(entity) * entry.pointsGainPerDifficulty();
        return Math.max(0.0D, points);
    }

    /**
     * 险地类概率翻倍：按位置难度把概率值折算成翻倍次数。
     * <p>难度阈值取 {@code difficulty_global.toml} 的通用档位：
     * 达到高档（默认 40）时概率取全值；达到中档（默认 20）时概率减半；低于中档不生效。
     * 该判定属于玩家侧加成，使用不含服务器难度偏移的原始难度。</p>
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
        double medium = MobDifficultyConfig.mediumThreshold();
        double high = MobDifficultyConfig.highThreshold();
        double chance = difficulty >= high ? value : difficulty >= medium ? value * 0.5D : 0.0D;
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
