package com.gam0zing.newnew_warlock_core.mob;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 被玩家击杀时的经验掉落缩放。
 * <p>掉落经验 = 原本该掉落的经验 × {@code max(1 + 该生物 points / 分母, 0)}。
 * 分母来自 {@code mob_rebuild/mobs.toml} 的全局项 {@code xp-points-divisor}，
 * 所有生物共用一条配置，配置 ≤ 0 时按 10 处理（见 {@link MobPointsConfig#xpPointsDivisor()}）。</p>
 * <p>只在玩家击杀时生效；自然死亡、岩浆等其它死因保持原版数值。</p>
 */
public final class MobExperienceEvt {

    private MobExperienceEvt() {}

    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        double points = ModAttributes.getValue(entity, ModAttributes.NEWNEW_POINTS);
        double multiplier = Math.max(1.0D + points / MobPointsConfig.xpPointsDivisor(), 0.0D);
        long dropped = Math.round(event.getDroppedExperience() * multiplier);
        event.setDroppedExperience((int) Math.min(dropped, Integer.MAX_VALUE));
    }
}
