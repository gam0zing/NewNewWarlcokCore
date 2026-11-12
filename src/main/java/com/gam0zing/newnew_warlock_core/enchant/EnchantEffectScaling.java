package com.gam0zing.newnew_warlock_core.enchant;

/**
 * 原版附魔效果的高等级扩展。
 * <p>原版有两处效果在 3 级之后会失去意义，本模组的可变等级上限必须把它们重新拉通：</p>
 * <ul>
 *     <li><b>饵钓</b>：{@code FishingHook} 里等待时间直接减去 {@code lureSpeed}，
 *     而 {@code rand(100, 600) - 100 * 等级} 在 6 级及以上永远小于等于 0，
 *     会每 tick 重新掷骰并永不咬钩。这里改为按等级比例递减，并在 1 秒处收敛。</li>
 *     <li><b>深海探索者</b>：原版每级给 {@code water_movement_efficiency} +1/3，
 *     而该属性的区间是 0~1，3 级即被 clamp。数据包把每级改为 0.2 后，
 *     属性超过 1 的部分在这里转换为继续加速，并对水中下落速度做逐级减免。</li>
 * </ul>
 * <p>所有公式都是纯函数，方便离线调参与测试。</p>
 */
public final class EnchantEffectScaling {

    // ============================== 饵钓 ==============================

    /** 原版饵钓每级折算的等待时间（5 秒 × 20 tick）。 */
    private static final double LURE_TICKS_PER_LEVEL = 100.0D;

    /** 每级等待时间的衰减系数；1~3 级结果与原版基本一致。 */
    private static final double LURE_WAIT_DECAY = 0.75D;

    /** 最短等待时间（1 秒），避免高等级直接跳过等待阶段。 */
    private static final int LURE_MIN_WAIT_TICKS = 20;

    /**
     * 计算浮漂的初始等待 tick。
     * <p>{@code baseWaitTicks} 是原版掷出的 {@code rand(100, 600)}，
     * {@code reductionTicks} 是 {@code EnchantmentHelper#getFishingTimeReduction} 换算后的 tick 数
     * （原版饵钓为每级 100 tick）。</p>
     * <p>等待时间按 {@code base * 0.75^等级} 递减，并保底 {@value #LURE_MIN_WAIT_TICKS} tick，
     * 因此任何等级都不会再出现负数重掷导致的永不咬钩。</p>
     */
    public static int lureWaitTicks(int baseWaitTicks, int reductionTicks) {
        if (reductionTicks <= 0) {
            return baseWaitTicks;
        }

        double levels = reductionTicks / LURE_TICKS_PER_LEVEL;
        int scaled = (int) Math.round(baseWaitTicks * Math.pow(LURE_WAIT_DECAY, levels));
        return Math.max(LURE_MIN_WAIT_TICKS, scaled);
    }

    // ============================== 深海探索者 ==============================

    /**
     * 深海探索者每级提供的 {@code water_movement_efficiency} 属性值。
     * <p>必须与 {@code data/minecraft/enchantment/depth_strider.json} 中的数值保持一致。</p>
     */
    public static final double DEPTH_STRIDER_PER_LEVEL = 0.2D;

    /** 属性值超过 1 之后的额外加速上限：最终等效属性趋近于 {@code 1 + 该值}。 */
    private static final double EXTRA_WATER_SPEED_HEADROOM = 1.0D;

    /** 额外加速的收敛速度，越小越早饱和。 */
    private static final double EXTRA_WATER_SPEED_CURVE = 0.75D;

    /** 每级额外降低的水中下落速度比例。 */
    private static final double WATER_SINK_REDUCTION_PER_LEVEL = 0.2D;

    /** 允许的属性上限：仍以原版 255 级硬上限换算，避免属性值被无意义地放大。 */
    public static final double WATER_EFFICIENCY_CAP =
        EnchantBaseline.LEVEL_HARD_CAP * DEPTH_STRIDER_PER_LEVEL;

    /**
     * 把属性原值转换为实际参与移动结算的等效效率。
     * <p>1 以内（约 5 级以内）保持线性：与原版 {@code LivingEntity#travelInWater} 的公式完全一致；
     * 超过 1 的部分按指数曲线收敛到 {@code 1 + 1}，即不疾跑时接近陆地行走、疾跑时接近陆地疾跑。</p>
     */
    public static double waterMovementEfficiency(double attributeValue) {
        double value = Math.max(0.0D, attributeValue);
        if (value <= 1.0D) {
            return value;
        }

        double extra = EXTRA_WATER_SPEED_HEADROOM
            * (1.0D - Math.exp(-(value - 1.0D) / EXTRA_WATER_SPEED_CURVE));
        return 1.0D + extra;
    }

    /**
     * 从属性原值反推深海探索者等级。
     * <p>等级与属性值的关系由附魔数据决定，这里是 0.2 × 等级，因此兼容数据包对该数值的调整。</p>
     */
    public static int depthStriderLevel(double attributeValue) {
        return (int) Math.round(Math.max(0.0D, attributeValue) / DEPTH_STRIDER_PER_LEVEL);
    }

    /**
     * 水中下落速度的保留比例：每级降低 20%，最低降到 0（不再下沉）。
     * <p>只作用于向下的速度，向上游动不受影响。</p>
     */
    public static double waterSinkScale(double attributeValue) {
        int level = depthStriderLevel(attributeValue);
        return Math.max(0.0D, 1.0D - WATER_SINK_REDUCTION_PER_LEVEL * level);
    }

    private EnchantEffectScaling() {}
}
