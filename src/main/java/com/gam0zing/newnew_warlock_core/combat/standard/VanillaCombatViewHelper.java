package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.combat.spine.AttackParticleType;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.spine.WarlockDamageHost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 原版战斗表现的统一出口。
 * <p>受伤表现、攻击音效、攻击粒子与工具耐久原本散落在原版各处；对应的调用点都保留了钩子，
 * 并把“是否放行原版表现”的决定集中到这里。</p>
 * <p>当前全部原样放行：先保证行为与原版一致，之后再按伤害类型区分。</p>
 */
public final class VanillaCombatViewHelper {

    /**
     * 受伤表现：受击动画、受伤/死亡音效。
     * <p>只有真正进入落地阶段且容器允许副作用的伤害才有表现，与原版 {@code hurtServer} 一致。</p>
     */
    public static void playHurtFeedback(DamageContext ctx) {
        LivingEntity target = ctx.getTarget();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        if (!ctx.isApplied() || !ctx.getContainer().shouldCauseSideEffects()) {
            return;
        }

        DamageSource source = ctx.getSource();
        WarlockDamageHost host = (WarlockDamageHost) target;

        // 客户端受击动画：实体红闪；受伤者本人客户端由此渲染红屏与镜头抖动
        level.broadcastDamageEvent(target, source);
        host.newnew$markHurt();

        // 受伤 / 死亡音效（原版在服务端播放，范围内玩家都能听到）
        if (target.isDeadOrDying()) {
            host.newnew$playDeathSound();
        } else {
            host.newnew$playHurtSound(source);
        }
    }

    /**
     * 玩家攻击音效（攻击、横扫、暴击、命中反馈）。
     *
     * @return {@code true} 放行原版音效；{@code false} 不播放
     */
    public static boolean handleAttackSound(Player player, SoundEvent sound) {
        return true;
    }

    /**
     * 玩家攻击粒子（暴击、附魔暴击、横扫、受击指示）。
     *
     * @return {@code true} 放行原版粒子；{@code false} 不发送
     */
    public static boolean handleAttackParticle(Player player, AttackParticleType type) {
        return true;
    }

    /**
     * 攻击方工具耐久（{@code ItemStack#hurtEnemy}）。
     *
     * @return {@code true} 放行原版，按物品自身逻辑扣耐久；{@code false} 不扣
     */
    public static boolean applyToolDurability(Player attacker, ItemStack tool, LivingEntity target) {
        return true;
    }

    private VanillaCombatViewHelper() {}
}
