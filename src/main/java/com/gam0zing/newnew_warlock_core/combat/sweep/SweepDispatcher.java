package com.gam0zing.newnew_warlock_core.combat.sweep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.gam0zing.newnew_warlock_core.combat.spine.AttackParticleType;
import com.gam0zing.newnew_warlock_core.combat.standard.VanillaCombatViewHelper;
import com.gam0zing.newnew_warlock_core.compat.bettercombat.BetterCombatGate;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * 横扫伤害分配器。
 * <p>原版把主目标直伤与横扫附加伤害分开结算；这里改为统一伤害池：</p>
 * <pre>
 * N     = 范围内的目标数（含主目标）
 * D     = 本次实际单体伤害
 * L     = 横扫之刃等级
 * total = N + (D - 1) * min(N, L + 1)
 * </pre>
 * <p>池内所有目标平均分摊 {@code total / N}。命中空气时仍以准心射线 1 格外作为横扫与波的起点，
 * 但不再虚构额外目标：名额与池子都只由真实目标决定。</p>
 */
public final class SweepDispatcher {

    /** 原版横扫对非主目标施加的击退强度。 */
    private static final double SWEEP_KNOCKBACK = 0.4D;
    /** 假设命中的虚拟目标尺寸。 */
    private static final double VIRTUAL_WIDTH = 0.6D;
    private static final double VIRTUAL_HEIGHT = 1.8D;
    /** 准心射线上虚拟目标的距离。 */
    private static final double VIRTUAL_DISTANCE = 1.0D;
    /** 空挥命中后补一次武器耐久与饥饿消耗，与原版攻击保持一致。 */
    private static final float EMPTY_SWING_EXHAUSTION = 0.1F;

    /**
     * 有目标时的横扫结算。
     *
     * @return 主目标是否受伤（与原版 {@code hurtOrSimulate} 的返回值语义一致）
     */
    public static boolean dispatch(ServerLevel level, Player player, LivingEntity primary,
        AABB sweepBox, float damage, DamageSource source) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(primary);
        collectTargets(level, player, sweepBox, primary, targets);

        int n = targets.size();
        int sweepingLevel = sweepingLevel(level, player);
        float share = share(n, damage, sweepingLevel);

        // 主目标没有受伤时与原版一致：不触发横扫后续
        if (!primary.hurtServer(level, source, share)) {
            return false;
        }
        double dealt = share;
        Set<UUID> damaged = new HashSet<>();
        damaged.add(primary.getUUID());
        // 主目标的击退与附魔附加效果由原版 attack 流程继续处理，这里只处理额外目标
        for (int i = 1; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            if (!target.hurtServer(level, source, share)) {
                continue;
            }
            dealt += share;
            damaged.add(target.getUUID());
            applySweepFeedback(level, player, target, source, share);
        }

        playSweepEffects(level, player);
        spawnWave(level, player, source, damage, sweepingLevel, dealt, damaged);
        return true;
    }

    /**
     * 无目标（砍空气）时的横扫入口。
     * <p>触发条件与原版横扫完全一致，只把「必须命中实体」替换为「准心射线 1 格外命中虚拟目标」。</p>
     *
     * @return 是否真的对至少一个实体造成了伤害
     */
    public static boolean dispatchEmptySwing(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        // Better Combat 接管该武器后，空挥也会走 BC 出手流程生成波；这里必须让路，避免一次挥砍出两个波
        if (BetterCombatGate.isWeapon(player.getWeaponItem())) {
            return false;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        ItemStack weapon = player.getWeaponItem();
        if (weapon.isEmpty() || !weapon.canPerformAction(ItemAbilities.SWORD_SWEEP)) {
            return false;
        }
        float scale = player.getAttackStrengthScale(0.5F);
        if (scale <= 0.9F) {
            return false;
        }
        if (player.isSprinting() || !player.onGround()) {
            return false;
        }
        if (player.getKnownMovement().horizontalDistanceSqr() >= Mth.square(player.getSpeed() * 2.5D)) {
            return false;
        }

        // 与原版攻击一致：条件成立即重置蓄力，避免空挥可以无冷却连发
        player.resetAttackStrengthTicker();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 center = eye.add(look.scale(VIRTUAL_DISTANCE));
        AABB virtual = new AABB(
            center.x - VIRTUAL_WIDTH / 2.0D, center.y - VIRTUAL_HEIGHT / 2.0D,
            center.z - VIRTUAL_WIDTH / 2.0D, center.x + VIRTUAL_WIDTH / 2.0D,
            center.y + VIRTUAL_HEIGHT / 2.0D, center.z + VIRTUAL_WIDTH / 2.0D);
        AABB sweepBox = virtual.inflate(1.0D, 0.25D, 1.0D);

        List<LivingEntity> targets = new ArrayList<>();
        collectTargets(level, player, sweepBox, null, targets);

        DamageSource source = weapon.getDamageSource(player);
        // 伤害加成基准从范围内的真实生物中随机取一只：不构造虚构实体，
        // 亡灵杀手等针对目标的加成仍然生效；范围内没有生物时以玩家自身兜底（无额外加成）
        Entity damageTarget = targets.isEmpty()
            ? player
            : targets.get(player.getRandom().nextInt(targets.size()));
        float damage = emptySwingDamage(level, player, weapon, scale, source, damageTarget);

        // 虚拟命中位置只用于确定横扫范围与波的起点，不占名额、不消耗池子
        int n = targets.size();
        int sweepingLevel = sweepingLevel(level, player);
        float share = share(n, damage, sweepingLevel);

        double dealt = 0.0D;
        Set<UUID> damaged = new HashSet<>();
        LivingEntity firstHurt = null;
        for (LivingEntity target : targets) {
            if (!target.hurtServer(level, source, share)) {
                continue;
            }
            dealt += share;
            damaged.add(target.getUUID());
            if (firstHurt == null) {
                firstHurt = target;
            }
            applySweepFeedback(level, player, target, source, share);
        }

        playSweepEffects(level, player);
        spawnWave(level, player, source, damage, sweepingLevel, dealt, damaged);

        if (firstHurt == null) {
            return false;
        }

        // 原版 attack 会为主目标扣一次武器耐久并消耗 0.1 饥饿值，空挥没有主目标，这里补上
        if (VanillaCombatViewHelper.applyToolDurability(player, weapon, firstHurt)) {
            weapon.hurtEnemy(firstHurt, player);
        }
        player.causeFoodExhaustion(EMPTY_SWING_EXHAUSTION);
        return true;
    }

    /**
     * Better Combat 出手结束后的波入口。
     * <p>Better Combat 已完成目标选择与区域伤害分摊，这里只处理未用满的额度：
     * 有实际命中时以实际单体伤害为基准，完全未命中时用「理论单体伤害 × 武器倍率」兜底。</p>
     */
    public static void spawnWaveForBetterCombat(ServerLevel level, Player player, float actualSingleDamage,
        double damageMultiplier, double dealt, Set<UUID> damaged) {
        ItemStack weapon = player.getWeaponItem();
        DamageSource source = weapon.getDamageSource(player);
        float damage = actualSingleDamage > 0.0F
            ? actualSingleDamage
            : emptySwingDamage(level, player, weapon, 1.0F, source, player) * (float) damageMultiplier;
        spawnWave(level, player, source, damage, sweepingLevel(level, player), dealt, damaged);
    }

    /** 收集范围内的额外目标（不含主目标与玩家自己）。 */
    private static void collectTargets(ServerLevel level, Player player, AABB sweepBox,
        LivingEntity excluded, List<LivingEntity> out) {
        double rangeSqr = Mth.square(player.entityInteractionRange());
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, sweepBox)) {
            if (candidate == excluded || !isValidTarget(player, candidate)) {
                continue;
            }
            if (player.distanceToSqr(candidate) >= rangeSqr) {
                continue;
            }
            out.add(candidate);
        }
    }

    private static boolean isValidTarget(Player player, LivingEntity candidate) {
        if (candidate == player || player.isAlliedTo(candidate)) {
            return false;
        }
        if (candidate.isSpectator() || !candidate.isAlive()) {
            return false;
        }
        // 原版横扫会跳过标记盔甲架
        return !(candidate instanceof ArmorStand stand && stand.isMarker());
    }

    /**
     * 统一伤害池：{@code N + (D - 1) * min(N, L + 1)}。
     */
    private static float share(int targetCount, float damage, int sweepingLevel) {
        if (targetCount <= 0) {
            return 0.0F;
        }
        int stacks = Math.min(targetCount, sweepingLevel + 1);
        float total = targetCount + (damage - 1.0F) * stacks;
        return total / targetCount;
    }

    private static int sweepingLevel(ServerLevel level, Player player) {
        ItemStack weapon = player.getWeaponItem();
        if (weapon.isEmpty()) {
            return 0;
        }
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = registry.getOrThrow(Enchantments.SWEEPING_EDGE);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, weapon);
    }

    /**
     * 空挥时的单体伤害：与原版 {@code attack} 的伤害链路一致，只是不含暴击（暴击不触发横扫）。
     */
    private static float emptySwingDamage(ServerLevel level, Player player, ItemStack weapon,
        float scale, DamageSource source, Entity damageTarget) {
        float raw = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // 附魔与物品加成都以实际结算对象为准，避免丢掉针对目标类型的加成（如亡灵杀手）
        float enchanted = EnchantmentHelper.modifyDamage(level, weapon, damageTarget, source, raw);
        float magicBoost = scale * (enchanted - raw);
        float base = raw * (0.2F + scale * scale * 0.8F);
        base += weapon.getItem().getAttackDamageBonus(damageTarget, base, source);
        return base + magicBoost;
    }

    private static void applySweepFeedback(ServerLevel level, Player player, LivingEntity target,
        DamageSource source, float damage) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        target.knockback(SWEEP_KNOCKBACK, Mth.sin(yaw), -Mth.cos(yaw), source, damage);
        EnchantmentHelper.doPostAttackEffects(level, target, source);
    }

    private static void playSweepEffects(ServerLevel level, Player player) {
        if (VanillaCombatViewHelper.handleAttackSound(player, SoundEvents.PLAYER_ATTACK_SWEEP)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
        }
        if (!VanillaCombatViewHelper.handleAttackParticle(player, AttackParticleType.SWEEP)) {
            return;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double xOffset = -Mth.sin(yaw);
        double zOffset = Mth.cos(yaw);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
            player.getX() + xOffset, player.getY(0.5D), player.getZ() + zOffset,
            0, xOffset, 0.0D, zOffset, 0.0D);
    }

    /** 普通横扫未吃满波池时，把剩余量交给飞行波。 */
    private static void spawnWave(ServerLevel level, Player player, DamageSource source, float damage,
        int sweepingLevel, double dealt, Set<UUID> damaged) {
        double percent = ModAttributes.getValue(player, ModAttributes.SWEEP_WAVE);
        if (percent <= 0.0D) {
            return;
        }
        double waveMax = percent * damage * (sweepingLevel + 1);
        double remaining = waveMax - dealt;
        if (remaining <= 0.0D) {
            return;
        }
        SweepWaveEntity wave = SweepEntities.SWEEP_WAVE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (wave == null) {
            return;
        }
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        wave.configure(player, direction.normalize(), waveMax, remaining, damage, source, damaged);
        level.addFreshEntity(wave);
    }

    private SweepDispatcher() {}
}
