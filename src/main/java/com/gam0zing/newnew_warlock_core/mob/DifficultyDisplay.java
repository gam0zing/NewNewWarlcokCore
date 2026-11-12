package com.gam0zing.newnew_warlock_core.mob;

/**
 * 客户端可见的当前位置总难度。
 * <p>数值由服务端每 tick 计算后同步过来（{@code mob.net.DifficultySync}），
 * 客户端只负责显示，不参与计算，因此包含传送门进度等仅服务端可见的因素。</p>
 * <p>{@code offset} 为服务器游戏难度带来的固定难度偏移（简单 -12 / 普通 0 / 困难 +12），
 * 仅随难度一起显示，不参与难度本身的计算。
 * {@code mediumThreshold}、{@code highThreshold} 为服务端下发的通用难度档位，用于颜色判定。</p>
 */
public final class DifficultyDisplay {

    /** 尚未收到服务端数值时以 {@link Double#NaN} 表示，HUD 此时不显示 */
    private static volatile double difficulty = Double.NaN;
    /** 服务器游戏难度偏移（和平/简单/普通/困难）；未同步前按 0 显示 */
    private static volatile double offset = 0.0D;
    /** 通用难度档位，未同步前与服务端默认值一致 */
    private static volatile double mediumThreshold = 20.0D;
    private static volatile double highThreshold = 40.0D;

    public static void set(
        double value, double offsetValue, double medium, double high) {
        difficulty = value;
        offset = offsetValue;
        mediumThreshold = medium;
        highThreshold = high;
    }

    /** {@return 当前位置总难度；尚未收到同步值时为 {@link Double#NaN}} */
    public static double get() {
        return difficulty;
    }

    /** {@return 服务器游戏难度偏移；尚未收到同步值时为 0} */
    public static double getOffset() {
        return offset;
    }

    /** {@return 中等难度档位（黄字阈值）} */
    public static double getMediumThreshold() {
        return mediumThreshold;
    }

    /** {@return 高难度档位（红字阈值）} */
    public static double getHighThreshold() {
        return highThreshold;
    }

    public static void reset() {
        difficulty = Double.NaN;
        offset = 0.0D;
        mediumThreshold = 20.0D;
        highThreshold = 40.0D;
    }

    private DifficultyDisplay() {}
}
