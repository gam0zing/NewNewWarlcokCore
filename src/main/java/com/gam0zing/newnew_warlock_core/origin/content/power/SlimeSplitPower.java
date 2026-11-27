package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.gam0zing.newnew_warlock_core.mixin.MobEffectInstanceStackAccess;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 史莱姆：受到致命伤害时分裂，以及粘液块带来的巨大化状态维护。
 * <p>分裂直接使用原版效果 {@code slime_split}：I 级 / II 级，持续 5 分钟；
 * 最大生命值的下降由效果等级推导，效果结束即恢复。</p>
 */
public final class SlimeSplitPower extends TraitPower {

    private static final String HEALTH_KEY = "slime/split_health";
    private static final String GIANT_HEALTH_KEY = "slime/giant_health";

    @Override
    public void onGranted(ServerPlayer player, NoConfig config) {
        refresh(player);
    }

    @Override
    public void onLogin(ServerPlayer player, NoConfig config) {
        refresh(player);
    }

    @Override
    public void onRespawn(ServerPlayer player, NoConfig config) {
        refresh(player);
    }

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        refresh(player);
    }

    @Override
    public void onRevoked(ServerPlayer player, NoConfig config) {
        clearModifier(player, Attributes.MAX_HEALTH, HEALTH_KEY);
        clearGiant(player);
        player.removeEffect(ModMobEffects.SLIME_SPLIT);
    }

    /**
     * 缩短当前分裂层的剩余时间（满血吃粘液球）。
     * <p>至少保留 1 tick，保证计时结束时仍会走效果自身的到期规则（II → I、重置 5 分钟），
     * 而不是把整层效果直接抹掉；改写后主动同步一次，避免客户端倒计时与服务端不一致。</p>
     */
    public static void shortenSplit(ServerPlayer player, int ticks) {
        MobEffectInstance split = player.getEffect(ModMobEffects.SLIME_SPLIT);
        if (split == null || ticks <= 0) {
            return;
        }
        int current = split.getDuration();
        if (current <= 0) {
            return;
        }
        int remaining = Math.max(1, current - ticks);
        if (remaining == current) {
            return;
        }
        ((MobEffectInstanceStackAccess) split).newnew$setDuration(remaining);
        player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), split, false));
    }

    /** 受到致命伤害时按分裂等级处理；返回 {@code false} 表示应当死亡。 */
    public static boolean trySplit(ServerPlayer player) {
        return performSplit(player) >= 0.0F;
    }

    /**
     * 执行一次分裂（致命分裂与主动分裂共用）。
     * <p>有巨大化时只移除巨大化；否则分裂等级 +1（无 → I → II），已达 II 级返回 -1。</p>
     *
     * @return 分裂前当前生命值中因最大生命值下降而被扣除的血量，最小为 0；
     *         返回 {@code -1} 表示已是 II 级分裂、无法再分裂
     */
    public static float performSplit(ServerPlayer player) {
        // 巨大化期间分裂：只清除巨大化，不增加分裂等级。
        if (SlimeState.isGiant(player)) {
            float healthBefore = player.getHealth();
            clearGiant(player);
            player.removeAllEffects();
            player.setHealth(player.getMaxHealth());
            playSplitSound(player);
            return Math.max(0.0F, healthBefore - player.getMaxHealth());
        }

        int level = splitLevel(player);
        if (level >= OriginTuning.SLIME_SPLIT_MAX_STACKS) {
            return -1.0F;
        }

        float healthBefore = player.getHealth();
        int next = level + 1;
        player.removeAllEffects();
        applyHealthModifiers(player, next);
        float lost = Math.max(0.0F, healthBefore - player.getMaxHealth());
        player.setHealth(player.getMaxHealth());
        applySplit(player, next);
        playSplitSound(player);
        return lost;
    }

    /** {@return 当前分裂等级：0 未分裂，1 为 I 级，2 为 II 级} */
    public static int splitLevel(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(ModMobEffects.SLIME_SPLIT);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    /** 主动分裂与致命分裂共用：播放一次原版史莱姆弹跳音效。 */
    private static void playSplitSound(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void applySplit(ServerPlayer player, int level) {
        player.addEffect(new MobEffectInstance(
            ModMobEffects.SLIME_SPLIT,
            OriginTuning.SLIME_SPLIT_DURATION_TICKS,
            level - 1,
            false,
            true,
            true));
    }

    private static void refresh(ServerPlayer player) {
        int level = splitLevel(player);
        if (level > 0) {
            applyHealthModifiers(player, level);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        } else {
            clearModifier(player, Attributes.MAX_HEALTH, HEALTH_KEY);
        }
        updateGiant(player, player.level().getGameTime());
    }

    private static void updateGiant(ServerPlayer player, long now) {
        long until = SlimeState.giantUntil(player);
        if (until <= 0L) {
            return;
        }
        if (now < until) {
            setModifier(player, Attributes.MAX_HEALTH, GIANT_HEALTH_KEY,
                OriginTuning.SLIME_GIANT_HEALTH_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            MobEffectInstance existing = player.getEffect(ModMobEffects.SLIME_GIANT);
            if (existing == null || existing.getDuration() < until - now) {
                player.addEffect(new MobEffectInstance(
                    ModMobEffects.SLIME_GIANT,
                    (int) (until - now),
                    0,
                    false,
                    true,
                    true));
            }
            return;
        }

        clearGiant(player);
    }

    /** 立即结算一次分裂/巨大化状态，用于刚施加巨大化后让生命上限马上到账。 */
    public static void refreshNow(ServerPlayer player) {
        refresh(player);
    }

    private static void clearGiant(ServerPlayer player) {
        clearModifier(player, Attributes.MAX_HEALTH, GIANT_HEALTH_KEY);
        SlimeState.clearGiant(player);
        player.removeEffect(ModMobEffects.SLIME_GIANT);
    }

    private static void applyHealthModifiers(ServerPlayer player, int stacks) {
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_KEY,
            Math.pow(OriginTuning.SLIME_SPLIT_HEALTH_FACTOR, stacks) - 1.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
