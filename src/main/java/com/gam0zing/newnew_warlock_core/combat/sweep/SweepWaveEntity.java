package com.gam0zing.newnew_warlock_core.combat.sweep;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 横扫之波：横扫结束后继续向前飞行的伤害波。
 * <p>波池总量 = 属性值 × D × (横扫之刃等级 + 1)。飞行中每格消耗 {@code 0.5 × D}，
 * 命中伤害 = {@code min(sqrt(剩余比例) × D, 剩余量)}；同一实体只命中一次。</p>
 * <p>速度随剩余比例开方衰减，剩余量归零或速度低于 0.05 时消失；被方块阻挡立即消失。</p>
 */
public class SweepWaveEntity extends Entity {

    /** 满池时的初速，约等于满蓄力箭矢。 */
    private static final double BASE_SPEED = 3.0D;
    /** 每飞行 1 格消耗的池量系数。 */
    private static final double TRAVEL_COST = 0.2D;
    private static final double MIN_SPEED = 0.05D;
    /** 实体检查盒：2.6 × 2.3 × 2.6，随速度方向水平旋转。 */
    private static final double HIT_BOX_WIDTH = 2.6D;
    private static final double HIT_BOX_HEIGHT = 2.3D;
    /** 弧光：一根平行于地面、垂直于速度方向、长 2.6 的弦，是两个圆弧的公共弦。 */
    private static final double CHORD_LENGTH = HIT_BOX_WIDTH;
    private static final double[] ARC_ANGLES = {25.0D, 75.0D};
    private static final int PARTICLES_PER_ARC = 10;

    private double waveMax = 1.0D;
    private double remaining;
    private float baseDamage;
    private Vec3 direction = Vec3.ZERO;
    private DamageSource source;
    private UUID shooterId;
    private final Set<UUID> hitTargets = new HashSet<>();

    public SweepWaveEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** 由横扫分配器写入本次波的全部初始状态。 */
    public void configure(Player shooter, Vec3 direction, double waveMax, double remaining,
        float baseDamage, DamageSource source, Set<UUID> alreadyDamaged) {
        this.shooterId = shooter.getUUID();
        this.direction = direction;
        this.waveMax = waveMax;
        this.remaining = remaining;
        this.baseDamage = baseDamage;
        this.source = source;
        this.hitTargets.addAll(alreadyDamaged);
        Vec3 center = shooter.getBoundingBox().getCenter();
        Vec3 spawn = center.add(direction.scale(0.125D));
        setPos(spawn.x, spawn.y - getBbHeight() / 2.0D, spawn.z);
        setNoGravity(true);
        setDeltaMovement(direction.scale(BASE_SPEED));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (source == null || waveMax <= 0.0D || remaining <= 0.0D) {
            discard();
            return;
        }
        double speed = speed();
        if (speed < MIN_SPEED) {
            discard();
            return;
        }

        Vec3 from = position();
        Vec3 motion = direction.scale(speed);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (horizontalCollision || verticalCollision) {
            discard();
            return;
        }

        // 每飞行 1 格消耗 0.5 × D
        remaining -= TRAVEL_COST * baseDamage * position().distanceTo(from);
        if (remaining <= 0.0D) {
            discard();
            return;
        }

        hitEntities(level);
        if (remaining <= 0.0D) {
            discard();
            return;
        }
        spawnArcParticles(level);
    }

    /** 当前速度：满池 3.0，按剩余比例开方衰减。 */
    private double speed() {
        return BASE_SPEED * Math.sqrt(remaining / waveMax);
    }

    private void hitEntities(ServerLevel level) {
        ServerPlayer shooter = level.getServer() == null || shooterId == null
            ? null : level.getServer().getPlayerList().getPlayer(shooterId);
        Vec3 center = position().add(0.0D, getBbHeight() / 2.0D, 0.0D);
        Vec3 forward = horizontalForward();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        // 搜索范围取旋转盒的外接 AABB，命中判定再按速度方向做水平旋转
        double reach = HIT_BOX_WIDTH / 2.0D * Math.sqrt(2.0D);
        AABB search = new AABB(
            center.x - reach, center.y - HIT_BOX_HEIGHT / 2.0D,
            center.z - reach, center.x + reach,
            center.y + HIT_BOX_HEIGHT / 2.0D, center.z + reach);
        List<Entity> candidates = level.getEntities(this, search, e -> e instanceof LivingEntity);
        for (Entity candidate : candidates) {
            if (!(candidate instanceof LivingEntity target)) {
                continue;
            }
            if (hitTargets.contains(target.getUUID())) {
                continue;
            }
            if (shooter != null && (shooter.isAlliedTo(target) || target.isSpectator())) {
                continue;
            }
            if (!intersectsRotatedBox(center, forward, right, target.getBoundingBox())) {
                continue;
            }
            double ratio = Math.sqrt(remaining / waveMax);
            float damage = (float) Math.min(ratio * baseDamage, remaining);
            if (damage <= 0.0F || !target.hurtServer(level, source, damage)) {
                continue;
            }
            hitTargets.add(target.getUUID());
            remaining -= damage;
            EnchantmentHelper.doPostAttackEffects(level, target, source);
            if (remaining <= 0.0D) {
                return;
            }
        }
    }

    /** 速度方向的水平投影，用于让伤害盒与粒子弦随速度方向水平旋转。 */
    private Vec3 horizontalForward() {
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return flat.normalize();
    }

    /**
     * 实体包围盒是否与随速度方向水平旋转的 2.6 × 2.3 × 2.6 盒相交。
     * <p>把实体包围盒的四个水平角点变换到波的本地方向后再做区间判定。</p>
     */
    private static boolean intersectsRotatedBox(Vec3 center, Vec3 forward, Vec3 right, AABB box) {
        double halfHeight = HIT_BOX_HEIGHT / 2.0D;
        if (box.maxY < center.y - halfHeight || box.minY > center.y + halfHeight) {
            return false;
        }
        double halfWidth = HIT_BOX_WIDTH / 2.0D;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (double x : new double[] {box.minX, box.maxX}) {
            for (double z : new double[] {box.minZ, box.maxZ}) {
                double dx = x - center.x;
                double dz = z - center.z;
                double localX = dx * right.x + dz * right.z;
                double localZ = dx * forward.x + dz * forward.z;
                minX = Math.min(minX, localX);
                maxX = Math.max(maxX, localX);
                minZ = Math.min(minZ, localZ);
                maxZ = Math.max(maxZ, localZ);
            }
        }
        return maxX >= -halfWidth && minX <= halfWidth && maxZ >= -halfWidth && minZ <= halfWidth;
    }

    /** 两条圆弧：以一根平行于地面、垂直于速度方向、长 2.6 的弦为公共弦，向飞行方向外凸。 */
    private void spawnArcParticles(ServerLevel level) {
        Vec3 center = position().add(0.0D, getBbHeight() / 2.0D, 0.0D);
        Vec3 forward = direction;
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();

        for (double degrees : ARC_ANGLES) {
            double half = Math.toRadians(degrees) / 2.0D;
            double radius = (CHORD_LENGTH / 2.0D) / Math.sin(half);
            // 圆心不在波中心：沿飞行反方向后退，使弦正好落在波中心上
            Vec3 circleCenter = center.subtract(forward.scale(radius * Math.cos(half)));
            for (int i = 0; i < PARTICLES_PER_ARC; i++) {
                double t = -half + 2.0D * half * ((double) i / (PARTICLES_PER_ARC - 1));
                Vec3 point = circleCenter
                    .add(right.scale(radius * Math.sin(t)))
                    .add(forward.scale(radius * Math.cos(t)));
                level.sendParticles(SweepParticles.SWEEP_WAVE.get(),
                    point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
