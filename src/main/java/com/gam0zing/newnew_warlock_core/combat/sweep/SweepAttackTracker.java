package com.gam0zing.newnew_warlock_core.combat.sweep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Better Combat 出手的伤害记录。
 * <p>Better Combat 自行完成目标选择与区域伤害分摊，我们不再干预，只在该次出手结束后
 * 用「实际造成的总伤害」和「实际受伤的实体」初始化横扫之波。</p>
 * <p>本类不引用任何 Better Combat 类型：只有 compat mixin 会写入，
 * 未安装 Better Combat 时表恒为空。</p>
 */
public final class SweepAttackTracker {

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** Better Combat 即将执行一次出手（空挥也会到达这里）。 */
    public static void begin(ServerPlayer player, int cursorTarget, double damageMultiplier) {
        PENDING.put(player.getUUID(), new Pending(cursorTarget, damageMultiplier));
    }

    /** 当前是否处于一次 Better Combat 出手的结算范围内。 */
    public static boolean isRecording(Player player) {
        return PENDING.containsKey(player.getUUID());
    }

    /** 记录一次成功的单体伤害。 */
    public static void record(Player player, LivingEntity target, float amount) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }
        pending.amounts.put(target.getUUID(), amount);
        if (target.getId() == pending.cursorTarget) {
            pending.cursorDamage = amount;
        }
    }

    /** 一次出手已经结算完毕：按剩余额度生成波。 */
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (UUID id : new ArrayList<>(PENDING.keySet())) {
            Pending pending = PENDING.remove(id);
            if (pending == null) {
                continue;
            }
            if (!(server.getPlayerList().getPlayer(id) instanceof ServerPlayer player)) {
                continue;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }
            double dealt = 0.0D;
            for (float value : pending.amounts.values()) {
                dealt += value;
            }
            SweepDispatcher.spawnWaveForBetterCombat(level, player, pending.singleTargetDamage(),
                pending.damageMultiplier, dealt, new HashSet<>(pending.amounts.keySet()));
        }
    }

    private static final class Pending {

        private final int cursorTarget;
        private final double damageMultiplier;
        private final Map<UUID, Float> amounts = new LinkedHashMap<>();
        private float cursorDamage;

        private Pending(int cursorTarget, double damageMultiplier) {
            this.cursorTarget = cursorTarget;
            this.damageMultiplier = damageMultiplier;
        }

        /** 本次实际单体伤害：优先准心目标，没有命中时返回 0 交给理论值兜底。 */
        private float singleTargetDamage() {
            if (cursorDamage > 0.0F) {
                return cursorDamage;
            }
            if (amounts.isEmpty()) {
                return 0.0F;
            }
            return Collections.max(amounts.values());
        }
    }

    private SweepAttackTracker() {}
}
