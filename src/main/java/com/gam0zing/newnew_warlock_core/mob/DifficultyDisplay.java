package com.gam0zing.newnew_warlock_core.mob;

/**
 * 客户端可见的当前位置总难度。
 * <p>数值由服务端每 tick 计算后同步过来（{@code mob.net.DifficultySync}），
 * 客户端只负责显示，不参与计算，因此包含传送门进度等仅服务端可见的因素。</p>
 */
public final class DifficultyDisplay {

    /** 尚未收到服务端数值时以 {@link Double#NaN} 表示，HUD 此时不显示 */
    private static volatile double difficulty = Double.NaN;

    public static void set(double value) {
        difficulty = value;
    }

    /** {@return 当前位置总难度；尚未收到同步值时为 {@link Double#NaN}} */
    public static double get() {
        return difficulty;
    }

    public static void reset() {
        difficulty = Double.NaN;
    }

    private DifficultyDisplay() {}
}
