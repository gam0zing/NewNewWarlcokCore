package com.gam0zing.newnew_warlock_core.compat.bettercombat;

/**
 * BC 左键攻击上下文。
 * <p>BC 的 {@code ServerNetwork#handleAttackRequest} 在一个同步任务里完成整套左键结算，
 * 这里用线程局部标记把这段时间圈出来，供 {@code ItemStack#getDamageSource} 判断
 * 「本次伤害是否来自 BC 左键」。</p>
 */
public final class SpearLiftContext {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    public static void enter() {
        ACTIVE.set(true);
    }

    public static void exit() {
        ACTIVE.set(false);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    private SpearLiftContext() {}
}
