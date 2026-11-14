package com.gam0zing.newnew_warlock_core.mob;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.Nullable;

/**
 * 客户端待显示的击杀点数提示队列。
 * <p>服务端下发 {@link com.gam0zing.newnew_warlock_core.mob.net.MobKillPointsPayload} 后，
 * 网络包处理器把条目放进这个队列；客户端 HUD 每帧取走并转成自己的动画条目。
 * 放在主源码集是为了让包处理器不直接引用客户端类。</p>
 */
public final class MobKillPointsDisplay {

    /** 一条待显示的获取记录 */
    public record Gain(double gained, double scale) {}

    private static final Queue<Gain> PENDING = new ConcurrentLinkedQueue<>();

    private MobKillPointsDisplay() {}

    public static void push(double gained, double scale) {
        PENDING.add(new Gain(gained, scale));
    }

    /** {@return 取走一条待显示记录；队列为空时返回 {@code null}} */
    @Nullable
    public static Gain poll() {
        return PENDING.poll();
    }

    public static void clear() {
        PENDING.clear();
    }
}
