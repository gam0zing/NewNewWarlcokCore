package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 玩家击杀使用 points 的生物时，按 {@code base_points × 缩放} 提高自己的属性点上限。
 * <p>累计值与属性修饰符由 {@link MobKillPoints} 负责持久化与重建；本类只负责判定击杀与计算本次获得的点数。
 * 缩放规则见 {@link MobPointsConfig#killPoints(net.minecraft.world.entity.EntityType, double, double)}。</p>
 * <p>只对能被玩家直接击杀的目标生效；没有 {@code newnew_points} 属性的生物（铁傀儡、雪傀儡等）不产出点数。</p>
 */
public final class MobKillPointsEvt {

    private MobKillPointsEvt() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead == player || dead.level().isClientSide()) {
            return;
        }
        AttributeInstance mobPoints = dead.getAttribute(ModAttributes.NEWNEW_POINTS);
        AttributeInstance playerPoints = player.getAttribute(ModAttributes.NEWNEW_POINTS);
        if (mobPoints == null || playerPoints == null) {
            return;
        }

        double gained = MobPointsConfig.killPoints(dead.getType(), mobPoints.getValue(), playerPoints.getValue());
        if (gained <= 0.0D) {
            return;
        }
        MobKillPoints.add(player, gained);
        MobKillPoints.applyModifier(player);
    }
}
