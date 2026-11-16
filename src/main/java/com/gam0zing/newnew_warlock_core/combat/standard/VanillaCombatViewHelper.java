package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.combat.spine.AttackParticleType;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.spine.WarlockDamageHost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 原版战斗表现的统一出口。
 * <p>受伤表现、攻击音效、攻击粒子与工具耐久原本散落在原版各处；对应的调用点都保留了钩子，
 * 并把“是否放行原版表现”的决定集中到这里。</p>
 * <p>受伤表现已按原版条件完整重放（格挡音/受击广播/受击音/二次音效），其余表现暂时原样放行。</p>
 */
public final class VanillaCombatViewHelper {

    /**
     * 受伤表现：受击动画、格挡音、受伤/死亡音效。
     * <p>只有真正进入落地阶段且容器允许副作用的伤害才有表现，与原版 {@code hurtServer} 一致。</p>
     * <p>分支也按原版：格挡成功只播盾牌格挡音、不广播伤害事件（因此没有红闪与镜头抖动）；
     * 未格挡才广播伤害事件，由客户端播放受击动画与本地受伤音。</p>
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
        // 原版以“实际格挡掉的伤害 > 0”判定 blocked：事件放行但减免为 0 时仍算未格挡
        boolean blocked = ctx.isShieldBlocked() && ctx.getBlockedDamage() > 0.0F;

        if (blocked) {
            // 盾牌格挡音：音效来自 blocks_attacks 组件的 blockSound（原版盾牌为 item.shield.block）
            host.newnew$playBlockSound(ctx.getShieldItem());
        } else {
            // 客户端受击表现：实体红闪、行走动画顿挫、玩家客户端红屏与镜头抖动、本地受伤音
            level.broadcastDamageEvent(target, source);
            // 受击方向指示：玩家会收到受击动画与方向箭头（原版在 dealDefaultKnockback 内触发）
            VanillaDamageUtils.indicateDamage(ctx);
        }

        // 受击状态标记：原版只对“有冲击且未被完全挡下”的伤害标记，完全格挡不进受击状态
        if (!source.is(DamageTypeTags.NO_IMPACT) && (!blocked || !ctx.isShieldFullyBlocked())) {
            host.newnew$markHurt();
        }

        // 受伤 / 死亡音效（原版在服务端播放，范围内玩家都能听到）
        // 伤害类型专属音（溺水、火焰、冰冻……）由 getHurtSound(source) 决定，已在 playHurtSound 内；
        // 荆棘反伤这类二次音效单独补一次
        if (target.isDeadOrDying()) {
            host.newnew$playDeathSound();
            host.newnew$playSecondaryHurtSound(source);
        } else {
            host.newnew$playHurtSound(source);
            host.newnew$playSecondaryHurtSound(source);
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
