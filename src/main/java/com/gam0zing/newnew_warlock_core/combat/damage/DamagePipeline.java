package com.gam0zing.newnew_warlock_core.combat.damage;

import com.gam0zing.newnew_warlock_core.combat.DamageTags;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsDamage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.ILivingEntityExtension;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * 伤害管线的业务区域。
 * <p>链路本身（{@code LivingEntityDamagePipelineMixin}、{@code LivingEntityKnockbackGateMixin}）
 * 只保留骨架与外部注入钩子：</p>
 * <ul>
 *     <li>骨架：容器数值同步、无敌帧计时、死亡分派、仇恨归因</li>
 *     <li>外部钩子：{@code LivingShieldBlockEvent}、{@code ArmorHurtEvent}、{@code LivingDamageEvent.Pre/Post}、
 *     {@code LivingKnockBackEvent}（击退插槽前重发）、{@code LivingUseTotemEvent}（免死插槽内）、
 *     {@code LivingDeathEvent}（死亡分派内）</li>
 * </ul>
 * <p>下面每个方法都是待实现的业务插槽，默认实现不产生任何原版伤害、扣血、击退或表现。</p>
 */
public final class DamagePipeline {

    // ==================================================================================
    // 插槽 0：内部伤害（入口短路）
    // ----------------------------------------------------------------------------------
    // inner_damage 标签的伤害不参与后续任何插槽：不经过防护、加成、无敌帧与伤害事件，
    // 按进入管线时的数值直接扣除生命值。
    // @return true 表示已在入口处理完毕，链路应直接返回
    // ==================================================================================
    public static boolean routeInnerDamage(DamageContext ctx) {
        if (!ctx.getSource().is(DamageTags.INNER_DAMAGE)) {
            return false;
        }
        ctx.setDealtDamage(InnerDamage.apply(ctx.getTarget(), ctx.getSource(), ctx.getIncomingDamage()));
        return true;
    }

    // ==================================================================================
    // 插槽 A：预处理
    // ----------------------------------------------------------------------------------
    // 伤害进入管线时的最早时机。适合做：伤害分类修正、来源/目标相关的标记、
    // 免疫/无效化判定、把事件里注入的数值整理成业务用结构等。
    // ==================================================================================
    public static void prepare(DamageContext ctx) {
        // 物理伤害：确定武器、受击部位与护甲（其余分类各自的预处理待实现）
        ctx.setPhysics(PhysicsDamage.prepare(ctx));
    }

    // ==================================================================================
    // 插槽 B：盾牌行为
    // ----------------------------------------------------------------------------------
    // 外部钩子 LivingShieldBlockEvent 已在链路中重发，格挡结果与格挡量已同步进容器：
    //   ctx.isShieldBlocked()、ctx.isShieldFullyBlocked()、ctx.getBlockedDamage()、
    //   ctx.getContainer().getShieldDamage()
    // 这里负责原版被剔除的部分：盾牌耐久、格挡音效/粒子、缴械、反弹、完全格挡时的表现等。
    // ==================================================================================
    public static void handleShieldBlock(DamageContext ctx) {
        // TODO: 业务区域 —— 格挡表现（音效、粒子、缴械、反弹、完全格挡时的表现等；耐久见插槽 B2）
    }

    // ==================================================================================
    // 插槽 B2：盾牌耐久（防守方）
    // ----------------------------------------------------------------------------------
    // 紧跟 LivingShieldBlockEvent 之后调用（仅本次盾牌确实格挡时进入）。
    //   ctx.getShieldDamage()  = LivingShieldBlockEvent#shieldDamage()（原版用它扣盾牌耐久）
    //   ctx.getBlockedDamage() = 实际格挡掉的伤害
    // 需要原版语义时：((WarlockDamageHost) ctx.getTarget()).newnew$hurtShield(ctx.getShieldDamage());
    // ==================================================================================
    public static void applyShieldDurability(DamageContext ctx) {
        // TODO: 业务区域 —— 盾牌耐久（当前正在使用的物品）
    }

    // ==================================================================================
    // 插槽 C：主伤害计算
    // ----------------------------------------------------------------------------------
    // @param damage 进入主计算时的伤害（已完成预处理与盾牌格挡）
    // @return 自定义规则算出的伤害（之后仍会经过 LivingDamageEvent.Pre）
    //
    // 原版的护甲、韧性、保护附魔、抗性提升等计算全部剔除，统一在这里实现：
    //   PHYSICS -> PhysicsWeapon 基准伤害 + PhysicsArmor 防护/击破/传递伤害
    //   MAGIC / FIRE / FROST / POISON / WITHER / ELECTRIC / NORMAL -> 各自规则
    //
    // 攻击者的伤害强度（damage_scale、<tag>_damage_scale）不在这里算：
    // 它们统一在 LivingDamageEvent.Pre 的 LOW 阶段修正（见 event.LivingDamageEvt），
    // 以便与其它模组的 Pre 修改、以及 LOWEST 阶段的限伤保持正确的先后顺序。
    //
    // 减免归属：若其中有“护甲 / 附魔 / 药水状态”吸收掉的部分，请分别通过
    //   ctx.setArmorReduction(...) / ctx.setEnchantmentReduction(...) / ctx.setMobEffectReduction(...)
    // 报告。这些数值会按原版分类写回 DamageContainer，让 LivingDamageEvent.Pre/Post 与其他模组
    // 读取到的 getReduction(...) 仍然有效，容器上通过 addModifier 注册的修正函数也会照常执行、
    // 其改动继续影响最终伤害。未归属的剩余减免由本管线自行消化。
    // ==================================================================================
    /**
     * 临时开关：为还没有专用逻辑的分类（魔法/火焰/冻结/中毒/凋零/雷电/普通）接回原版减伤。
     * <p>改回 {@code false} 即恢复原来的“裸数值透传”。</p>
     */
    public static boolean vanillaDamageForUnhandled = true;

    public static float calculateMainDamage(DamageContext ctx, float damage) {
        return switch (ctx.getCategory()) {
            case PHYSICS -> PhysicsDamage.calculate(ctx, damage);
            // 临时：其余分类接回原版减伤；关闭开关后恢复原样透传
            default -> vanillaDamageForUnhandled ? vanillaDamage(ctx, damage) : damage;
        };
    }

    /**
     * 原版非物理伤害的减伤链路：护甲 + 韧性 → 保护附魔 / 抗性提升。
     * <p>{@code getDamageAfterArmorAbsorb} 内部还会按原版规则消耗护甲耐久并发出 {@code ArmorHurtEvent}，
     * 因此插槽 D 不需要为这些分类再扣一次。</p>
     */
    private static float vanillaDamage(DamageContext ctx, float damage) {
        WarlockDamageHost host = (WarlockDamageHost) ctx.getTarget();
        float afterArmor = host.newnew$getDamageAfterArmorAbsorb(ctx.getSource(), damage);
        return host.newnew$getDamageAfterMagicAbsorb(ctx.getSource(), afterArmor);
    }

    // ==================================================================================
    // 插槽 D：护甲耐久（防守方）
    // ----------------------------------------------------------------------------------
    // 对应原版 actuallyHurt 里“扣护甲耐久”的部分；默认什么都不做（护甲不掉耐久，
    // ArmorHurtEvent 也不会发出）。
    // 原版规则：BYPASSES_ARMOR 的伤害不扣护甲耐久；每 4 点伤害扣 1 点耐久（至少 1），
    // 最终数值由 ArmorHurtEvent 逐槽位决定（可取消、可改）。
    // 需要原版语义（同时保留 ArmorHurtEvent 兼容）时：
    //   if (ctx.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;
    //   ((WarlockDamageHost) ctx.getTarget()).newnew$hurtArmorItems(ctx.getSource(), 伤害值);
    // 只扣头盔（对应原版 DAMAGES_HELMET）：newnew$hurtHelmetItem(...)
    // ==================================================================================
    public static void applyArmorDurability(DamageContext ctx) {
        // 物理伤害：按结算出的耐久损耗扣除被命中护甲（不可破坏装备自动忽略）
        PhysicsDamage.applyArmorDurability(ctx);
    }

    // ==================================================================================
    // 插槽 E：落地
    // ----------------------------------------------------------------------------------
    // 在 LivingDamageEvent.Pre 之后、LivingDamageEvent.Post 之前调用，负责把伤害真正作用到生物身上。
    // 原版 actuallyHurt 中被剔除的部分都归这里，默认什么都不做（生物不会掉血），
    // 需要时从这里自行实现：
    //   1. 伤害吸收：container.setReduction(DamageContainer.Reduction.ABSORPTION, absorbed)，
    //      并从 self.getAbsorptionAmount() 里扣除（ctx.setAbsorbedDamage(...) 记录实际吸收量）。
    //   2. 扣血：self.getCombatTracker().recordDamage(source, damage)、
    //      self.setHealth(self.getHealth() - damage)、self.gameEvent(GameEvent.ENTITY_DAMAGE)、
    //      ((ILivingEntityExtension) self).onDamageTaken(container)。
    //   3. 其他落地副作用：生命值上限/阶段血条、伤害累积、标记位等。
    // @param damage LivingDamageEvent.Pre 之后的伤害（可与 ctx.getFinalDamage() 对照）
    // ==================================================================================
    public static void applyDamage(DamageContext ctx, float damage) {
        LivingEntity self = ctx.getTarget();
        DamageSource source = ctx.getSource();
        DamageContainer container = ctx.getContainer();

        float remaining = Math.max(0.0F, damage);

        // 伤害吸收
        float absorbed = 0.0F;
        if (remaining > 0.0F && self.getAbsorptionAmount() > 0.0F) {
            absorbed = Math.min(remaining, self.getAbsorptionAmount());
            container.setReduction(DamageContainer.Reduction.ABSORPTION, absorbed);
            self.setAbsorptionAmount(self.getAbsorptionAmount() - absorbed);
            remaining -= absorbed;
        }
        ctx.setAbsorbedDamage(absorbed);

        if (remaining <= 0.0F) {
            return;
        }

        // 扣血与战斗记录
        float healthBefore = self.getHealth();
        self.getCombatTracker().recordDamage(source, remaining);
        self.setHealth(healthBefore - remaining);
        self.gameEvent(GameEvent.ENTITY_DAMAGE);
        ((ILivingEntityExtension) self).onDamageTaken(container);

        // 物理伤害：受击部位的附加效果按实际扣血概率触发
        PhysicsDamage.applyBodyEffects(ctx, remaining, healthBefore);
    }

    // ==================================================================================
    // 插槽 F：击退
    // ----------------------------------------------------------------------------------
    // 原版 LivingEntity#knockback(double,double,double) 被拦截后转到此处（所有来源都会经过：
    // 玩家近战/横扫、生物攻击、山羊冲撞、末影龙等）。
    // LivingKnockBackEvent 已在链路中重发（其他模组的取消与数值改写仍然生效），
    // 这里拿到的 event 就是最终强度；默认什么都不做，因此没有击退。
    //
    // 需要原版效果时可参考：
    //   double s = event.getStrength() * (1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    //   if (s <= 0.0) return;
    //   target.hasImpulse = true;
    //   Vec3 d = target.getDeltaMovement();
    //   Vec3 dir = new Vec3(event.getRatioX(), 0.0, event.getRatioZ()).normalize().scale(s);
    //   target.setDeltaMovement(d.x / 2.0 - dir.x,
    //                           target.onGround() ? Math.min(0.4, d.y / 2.0 + s) : d.y,
    //                           d.z / 2.0 - dir.z);
    // ==================================================================================
    public static void handleKnockback(LivingEntity target, LivingKnockBackEvent event) {
        // TODO: 业务区域 —— 击退
    }

    // ==================================================================================
    // 插槽 G：受伤表现
    // ----------------------------------------------------------------------------------
    // 落地之后调用，负责所有视听与反馈：
    //   受伤音效（playHurtSound）、受伤动画（hurtDuration/hurtTime）、level.broadcastDamageEvent /
    //   完全格挡的 (byte)29、hurtMarked、indicateDamage、粒子、死亡音效（self.isDeadOrDying() 时）等。
    // ctx.isApplied() 表示本次是否真正进入落地阶段（无敌帧不足时为 false）。
    // ==================================================================================

    /**
     * 临时开关：恢复原版受伤表现（受伤音效、实体红闪、玩家红屏）。
     * <p>改回 {@code false} 即等价于原来的空钩子（无音效、无变色、无屏幕效果）。</p>
     */
    public static boolean vanillaFeedback = true;

    public static void playFeedback(DamageContext ctx) {
        // 临时：恢复原版受伤表现。重新屏蔽时把 vanillaFeedback 改回 false 即可。
        if (!vanillaFeedback) {
            return;
        }
        LivingEntity self = ctx.getTarget();
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        // 与 hurtServer 一致：无敌帧内没有触发落地阶段的伤害不产生任何表现
        if (!ctx.isApplied() || !ctx.getContainer().shouldCauseSideEffects()) {
            return;
        }

        DamageSource source = ctx.getSource();
        WarlockDamageHost host = (WarlockDamageHost) self;

        // 客户端受击动画：实体红闪；受伤者本人客户端由此渲染红屏与镜头抖动
        level.broadcastDamageEvent(self, source);
        host.newnew$markHurt();

        // 受伤 / 死亡音效（原版在服务端播放，范围内玩家都能听到）
        if (self.isDeadOrDying()) {
            host.newnew$playDeathSound();
        } else {
            host.newnew$playHurtSound(source);
        }
    }

    // ==================================================================================
    // 插槽 H：免死
    // ----------------------------------------------------------------------------------
    // 在 self.isDeadOrDying() 之后、self.die(source) 之前调用。
    // @return true 表示本次免死（链路会跳过 die）。
    // 外部钩子 LivingUseTotemEvent 的重发位置就在这里（原版在 checkTotemDeathProtection 内发出）；
    // 图腾、免死效果、复活类能力都在这个插槽实现。
    // ==================================================================================
    public static boolean tryPreventDeath(DamageContext ctx) {
        // TODO: 业务区域 —— 图腾/免死判定（重发 LivingUseTotemEvent）
        return false;
    }

    // ==================================================================================
    // 插槽 I：伤害后处理
    // ----------------------------------------------------------------------------------
    // 链路最后一站（死亡分派之后）。原版被剔除的收尾逻辑都在这里补：
    //   状态效果回调 effect.onMobHurt(this, source, amount)、
    //   lastDamageSource / lastDamageStamp、
    //   统计 Stats.DAMAGE_TAKEN / DAMAGE_ABSORBED / DAMAGE_DEALT_ABSORBED /
    //   DAMAGE_RESISTED / DAMAGE_DEALT_RESISTED / DAMAGE_BLOCKED_BY_SHIELD、
    //   进度触发器 ENTITY_HURT_PLAYER / PLAYER_HURT_ENTITY、
    //   食物消耗 causeFoodExhaustion 等。
    // ==================================================================================
    public static void onDamageApplied(DamageContext ctx) {
        // TODO: 业务区域 —— 伤害后处理（状态效果回调、统计、进度、食物消耗等）
    }

    // ==================================================================================
    // 插槽 J：玩家攻击音效
    // ----------------------------------------------------------------------------------
    // 原版 Player#attack 内部直接调用 level().playSound(...) 播放的攻击音效，全部转到这里：
    //   PLAYER_ATTACK_NODAMAGE（格挡弹开/伤害为 0）、PLAYER_ATTACK_KNOCKBACK（冲刺）、
    //   PLAYER_ATTACK_SWEEP（横扫）、PLAYER_ATTACK_CRIT（暴击）、
    //   PLAYER_ATTACK_STRONG / PLAYER_ATTACK_WEAK（普通命中）。
    // @return true 表示放行原版音效；false 表示不播放（默认）。
    // ==================================================================================
    public static boolean handleAttackSound(Player player, SoundEvent sound) {
        // TODO: 业务区域 —— 玩家攻击音效
        return false;
    }

    // ==================================================================================
    // 插槽 K：玩家攻击粒子
    // ----------------------------------------------------------------------------------
    // 原版 Player#attack / ServerPlayer 里发出的攻击粒子，全部转到这里：
    //   CRIT             -> ServerPlayer#crit（ClientboundAnimatePacket 4）
    //   MAGIC_CRIT       -> ServerPlayer#magicCrit（ClientboundAnimatePacket 5）
    //   SWEEP            -> Player#sweepAttack（SWEEP_ATTACK 粒子）
    //   DAMAGE_INDICATOR -> Player#attack 里的 DAMAGE_INDICATOR 粒子（受击指示）
    // @return true 表示放行原版粒子；false 表示不发（默认）。
    // ==================================================================================
    public static boolean handleAttackParticle(Player player, AttackParticleType type) {
        // TODO: 业务区域 —— 玩家攻击粒子
        return false;
    }

    // ==================================================================================
    // 插槽 L：工具耐久（攻击方）
    // ----------------------------------------------------------------------------------
    // 玩家近战命中后，原版在 Player#attack 里调用 ItemStack#hurtEnemy(target, player) 扣武器耐久：
    // 内部走物品自身的逻辑（可被自定义物品覆写）、并记录攻击者统计、处理损坏与 postHurtEnemy。
    // @return true 表示放行原版（按物品自身逻辑扣耐久）；false 表示不扣（默认）。
    // 需要自行结算时可调用 tool.hurtAndBreak(数量, attacker, EquipmentSlot.MAINHAND) 再返回 false。
    // ==================================================================================
    public static boolean applyToolDurability(Player attacker, ItemStack tool, LivingEntity target) {
        // TODO: 业务区域 —— 工具/武器耐久
        return false;
    }

    private DamagePipeline() {}
}
