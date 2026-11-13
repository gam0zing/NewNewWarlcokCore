package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 玩家击杀使用 points 的生物时，按 {@code base_points × 缩放} 提高自己的属性点上限。
 * <p>所有击杀加成集中在玩家身上唯一一个 {@code newnew_warlock_core:mob_kill_points} 修饰符里，
 * 操作类型固定为 {@link AttributeModifier.Operation#ADD_VALUE}；每次击杀把本次获得的点数累加进修饰符。
 * 缩放规则见 {@link MobPointsConfig#killPoints(net.minecraft.world.entity.EntityType, double, double)}。</p>
 * <p>只对能被玩家直接击杀的目标生效；没有 {@code newnew_points} 属性的生物（铁傀儡、雪傀儡等）不产出点数。</p>
 */
public final class MobKillPointsEvt {

    /** 玩家身上管理击杀加成的唯一修饰符 */
    private static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "mob_kill_points");

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
        AttributeModifier existing = playerPoints.getModifier(MODIFIER_ID);
        double total = (existing != null ? existing.amount() : 0.0D) + gained;
        playerPoints.addOrReplacePermanentModifier(
            new AttributeModifier(MODIFIER_ID, total, AttributeModifier.Operation.ADD_VALUE));
    }
}
