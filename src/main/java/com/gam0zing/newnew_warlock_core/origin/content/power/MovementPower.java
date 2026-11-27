package com.gam0zing.newnew_warlock_core.origin.content.power;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;

/**
 * 种族移动规则中需要逐 tick 处理的部分。
 * <p>贴墙减速与蹬墙跳必须在客户端结算（服务端不模拟玩家水平碰撞，也无法可靠地改玩家速度），
 * 这里只负责按同样的状态条件扣除饥饿值。</p>
 */
final class MovementPower extends TraitPower {

    enum Flag {
        /** 猫：贴墙下落与蹬墙跳的饥饿消耗 */
        CAT_WALL,
        /** 猫：水下氧气消耗加快 */
        CAT_OXYGEN
    }

    /** 记录上一 tick 的跳跃键状态，用于识别按键上升沿。 */
    private static final Map<ServerPlayer, Boolean> LAST_JUMP = new WeakHashMap<>();

    private final Set<Flag> flags;

    MovementPower(Flag... flags) {
        this.flags = EnumSet.noneOf(Flag.class);
        this.flags.addAll(Set.of(flags));
    }

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        if (flags.contains(Flag.CAT_WALL)) {
            catWallCost(player);
        }
        if (flags.contains(Flag.CAT_OXYGEN)) {
            drainOxygen(player);
        }
    }

    private static void catWallCost(ServerPlayer player) {
        boolean jumpPressed = updateJumpPress(player);
        if (player.onGround() || player.isPassenger() || player.isSpectator() || player.isDeadOrDying()
            || player.isFallFlying() || player.onClimbable()
            || player.isInWater() || player.isInLava()
            || !player.getLastClientInput().forward()) {
            return;
        }
        if (!OriginTraits.isWallAhead(player)) {
            return;
        }
        if (OriginTraits.catWallSlideFactor(player) >= 1.0D) {
            return;
        }
        // 饥饿值不足 6 时无法蹬墙跳，因此也不扣跳跃消耗。
        if (jumpPressed && player.getFoodData().getFoodLevel() > 6) {
            player.getFoodData().addExhaustion(OriginTuning.CAT_WALL_JUMP_EXHAUSTION);
        }
        if (player.tickCount % 20 == 0) {
            player.getFoodData().addExhaustion(OriginTuning.CAT_WALL_EXHAUSTION_PER_SECOND);
        }
    }

    private static boolean updateJumpPress(ServerPlayer player) {
        boolean jump = player.getLastClientInput().jump();
        Boolean previous = LAST_JUMP.put(player, jump);
        return jump && Boolean.FALSE.equals(previous);
    }

    private static void drainOxygen(ServerPlayer player) {
        if (!player.isEyeInFluid(FluidTags.WATER) || player.tickCount % 2 != 0) {
            return;
        }
        player.setAirSupply(player.getAirSupply() - 1);
    }
}
