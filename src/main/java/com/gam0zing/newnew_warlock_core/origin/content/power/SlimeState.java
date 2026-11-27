package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;

/**
 * 史莱姆巨大化的跨登录状态。
 * <p>到期时间写在玩家持久化 NBT 里，因此死亡、重登、跨维度都不会丢失；
 * 分裂状态则直接使用原版效果，不经过这里。</p>
 */
public final class SlimeState {

    private static final String GIANT_UNTIL = "NewNewSlimeGiantUntil";

    public static long giantUntil(ServerPlayer player) {
        return player.getPersistentData().getLong(GIANT_UNTIL).orElse(0L);
    }

    public static void setGiantUntil(ServerPlayer player, long until) {
        player.getPersistentData().putLong(GIANT_UNTIL, until);
    }

    public static void clearGiant(ServerPlayer player) {
        player.getPersistentData().remove(GIANT_UNTIL);
    }

    /** {@return 当前是否处于巨大化持续时间内} */
    public static boolean isGiant(ServerPlayer player) {
        long until = giantUntil(player);
        return until > 0L && player.level().getGameTime() < until;
    }

    private SlimeState() {}
}
