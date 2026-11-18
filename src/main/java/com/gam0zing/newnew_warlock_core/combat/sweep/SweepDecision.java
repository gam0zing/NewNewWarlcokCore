package com.gam0zing.newnew_warlock_core.combat.sweep;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;

/**
 * 本次攻击是否属于横扫的临时标记。
 * <p>原版在 {@code hurtOrSimulate} 之前触发 {@link SweepAttackEvent}，而伤害分配发生在
 * {@code hurtOrSimulate} 调用点；两边没有直接的参数传递通道，因此按玩家 UUID 暂存一次，
 * 由攻击 Mixin 在该调用点取走。</p>
 * <p>每次攻击都会触发该事件：非横扫会显式清除标记，因此上一次攻击遗留的标记不可能被下一次攻击消费。</p>
 */
public final class SweepDecision {

    private static final Set<UUID> SWEEPING = ConcurrentHashMap.newKeySet();

    public static void onSweepAttack(SweepAttackEvent event) {
        Player player = event.getEntity();
        // 客户端也会走 Player#attack 并触发该事件；标记只在服务端有效，
        // 否则本地客户端与集成服务端会互相抢走对方的标记
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }
        if (event.isSweeping()) {
            SWEEPING.add(player.getUUID());
        } else {
            SWEEPING.remove(player.getUUID());
        }
    }

    /**
     * 取走并清除标记。
     *
     * @return 本次攻击是否为横扫
     */
    public static boolean consume(Player player) {
        return SWEEPING.remove(player.getUUID());
    }

    private SweepDecision() {}
}
