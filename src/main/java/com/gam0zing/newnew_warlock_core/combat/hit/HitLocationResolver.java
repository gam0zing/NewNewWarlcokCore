package com.gam0zing.newnew_warlock_core.combat.hit;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.gam0zing.newnew_warlock_core.combat.damage.DamageCategory;

/**
 * 受击位置解析。
 * <p>把“这次伤害打在目标的哪个面、面上哪个点”解析成 {@link HitLocation}。
 * 结果使用生物自身的相对面（见 {@link HitFace}），与面在世界里的朝向无关。</p>
 *
 * <p><b>为什么不用 AABB 直接求交</b>：轴对齐碰撞箱不会跟着生物转身，斜向的连线会打在“世界坐标看着对、
 * 相对生物却不对”的位置。因此这里先把生物当前的 AABB 绕竖直轴旋转到它的身体朝向，得到一个虚拟的
 * “朝向盒”（见 {@link TargetBox}），所有求交都针对这个盒子。</p>
 *
 * <p>按伤害来源给出若干<b>候选射线</b>，依次与朝向盒求交（第一个命中的候选胜出）：</p>
 * <ol>
 *     <li><b>弹射物</b>：弹射物碰撞箱中心 → 朝向盒中心的连线。</li>
 *     <li><b>非玩家生物造成的非弹射物物理伤害</b>：从攻击者中心出发，相对“攻击者中心 → 朝向盒中心”
 *     的连线偏转一个随机方向、0-30° 随机角度的射线；中心连线本身作为第二条候选。</li>
 *     <li><b>玩家造成的非弹射物物理伤害</b>：玩家视野中心（视点 + 视线方向）；玩家中心 → 朝向盒中心的
 *     连线作为第二条候选。</li>
 * </ol>
 * <p>其余情况（非物理伤害、环境伤害等）使用“来源中心 → 朝向盒中心”的连线；连可用的起点都没有时返回
 * {@link Optional#empty()}。</p>
 *
 * <p><b>两条兜底（朝向盒与 AABB 的边界并不重合，见 {@link TargetBox}）</b>：</p>
 * <ol>
 *     <li>命中朝向盒、但没有任何部位覆盖该点：由调用方退回全局默认部位（倍率 1、无效果），
 *     见 {@link com.gam0zing.newnew_warlock_core.body.BodyParts#find}。</li>
 *     <li>连朝向盒都没命中（常见于打中 AABB 的四个角）：先用该射线与 AABB 求交得到交点，再从该交点
 *     朝朝向盒中心连线、与朝向盒求交，用这个交点所在的面与面内坐标。这一步只在上面所有候选都落空时执行。</li>
 *     <li>如果连 AABB 都没命中（或根本没有可用射线）：返回 {@link Optional#empty()}，由调用方改用该生物
 *     数据文件里配置的默认受击部位（{@code default_part}），最后才退回全局默认部位。</li>
 * </ol>
 * <p>若重投影的起点恰好已经落在朝向盒内（AABB 的角比朝向盒更靠外时会发生），则直接取该起点并归到最近的面。</p>
 *
 * <p><b>面内坐标约定</b>：{@code x}、{@code y} 以“从体外正对该面看”的方向取左下角为 {@code (0, 0)}、
 * 右上角为 {@code (1, 1)}；侧面（左/右/前/后）的“上”是世界竖直向上，上/下面的“上”是生物自身的前方。</p>
 */
public final class HitLocationResolver {

    /** 随机偏转角上限（度），实际取 {@code [0, 30)} */
    private static final double MAX_DEFLECTION_DEGREES = 30.0D;

    private HitLocationResolver() {}

    /**
     * 解析受击位置（使用目标自身的随机源）。
     *
     * @param source 造成这次伤害的伤害来源，可为 null
     * @param target 受击目标
     * @return 受击位置；无法解析时为 {@link Optional#empty()}
     */
    public static Optional<HitLocation> resolve(@Nullable DamageSource source, LivingEntity target) {
        return resolve(source, target, target.getRandom());
    }

    /**
     * 解析受击位置。
     *
     * @param source 造成这次伤害的伤害来源，可为 null
     * @param target 受击目标
     * @param random 随机源（用于情况 2 的随机偏转；结果需要可复现时请传入确定的随机源）
     * @return 受击位置；无法解析时为 {@link Optional#empty()}
     */
    public static Optional<HitLocation> resolve(
        @Nullable DamageSource source, LivingEntity target, RandomSource random) {
        AABB aabb = target.getBoundingBox();
        TargetBox box = TargetBox.of(aabb, target.getYRot());
        List<Ray> rays = candidateRays(source, target, box, random);
        if (rays.isEmpty()) {
            return Optional.empty();
        }

        // 1）先与朝向盒求交：这是最符合直觉的结果
        for (Ray ray : rays) {
            TargetBox.FaceHit hit = box.clip(ray.start(), ray.direction());
            if (hit != null) {
                return Optional.of(box.locationOf(hit));
            }
        }

        // 2）全部落空：用 AABB 的交点朝盒中心重投影（只在罕见情况下才会走到这里）
        for (Ray ray : rays) {
            TargetBox.FaceHit hit = reproject(aabb, box, ray);
            if (hit != null) {
                return Optional.of(box.locationOf(hit));
            }
        }
        return Optional.empty();
    }

    // ==========================================================================================
    // 候选射线
    // ==========================================================================================

    /**
     * 按伤害来源给出候选射线，越靠前越可信。
     * <p>返回空列表表示没有任何可用的实体来源（此时调用方会退回该生物的默认受击部位）。</p>
     */
    private static List<Ray> candidateRays(
        @Nullable DamageSource source, LivingEntity target, TargetBox box, RandomSource random) {
        Entity direct = source == null ? null : source.getDirectEntity();
        Entity causing = source == null ? null : source.getEntity();
        Vec3 center = box.center();

        // 情况 1：弹射物 —— 弹射物中心与目标中心的连线
        if (direct instanceof Projectile || (source != null && source.is(DamageTypeTags.IS_PROJECTILE))) {
            Entity projectile = direct != null ? direct : causing;
            if (projectile != null && projectile != target) {
                Vec3 origin = projectile.getBoundingBox().getCenter();
                return List.of(new Ray(origin, center.subtract(origin)));
            }
        }

        // 情况 3：玩家造成的非弹射物物理伤害 —— 视野中心，其次中心连线
        if (causing instanceof Player player && player != target) {
            if (isPhysical(source)) {
                return List.of(
                    new Ray(player.getEyePosition(), player.getViewVector(1.0F)),
                    fromCenter(player, center));
            }
            return List.of(fromCenter(player, center));
        }

        // 情况 2：非玩家生物造成的非弹射物物理伤害 —— 随机偏转射线，其次中心连线
        if (causing instanceof LivingEntity attacker && attacker != target) {
            if (isPhysical(source)) {
                Vec3 origin = attacker.getBoundingBox().getCenter();
                Vec3 straight = center.subtract(origin);
                return List.of(
                    new Ray(origin, deflect(straight, random)),
                    new Ray(origin, straight));
            }
            return List.of(fromCenter(attacker, center));
        }

        // 兜底：直接来源（或攻击者）中心 → 目标中心
        Entity origin = direct != null ? direct : causing;
        if (origin != null && origin != target) {
            return List.of(fromCenter(origin, center));
        }
        return List.of();
    }

    /** “来源碰撞箱中心 → 朝向盒中心”的射线 */
    private static Ray fromCenter(Entity from, Vec3 center) {
        Vec3 origin = from.getBoundingBox().getCenter();
        return new Ray(origin, center.subtract(origin));
    }

    /**
     * 朝向盒没被命中时的重投影：先取射线与 AABB 的交点，再从这个交点朝盒中心连线与朝向盒求交。
     *
     * @return 重投影得到的命中结果；射线连 AABB 都没命中时返回 null
     */
    @Nullable
    private static TargetBox.FaceHit reproject(AABB aabb, TargetBox box, Ray ray) {
        RayHit aabbHit = clipRay(aabb, ray.start(), ray.direction());
        if (!aabbHit.isHit()) {
            return null;
        }
        Vec3 point = aabbHit.point();
        return box.clip(point, box.center().subtract(point));
    }

    // ==========================================================================================
    // 方向
    // ==========================================================================================

    /**
     * 从中心连线出发，偏转一个随机方向、{@code 0-30°} 的随机角度。
     * <p>角度可以取到 0，因此偏转结果永远落在“原点连线上、可能命中目标”的锥体内。</p>
     */
    private static Vec3 deflect(Vec3 direction, RandomSource random) {
        if (direction.lengthSqr() < 1.0E-8D) {
            return direction;
        }
        Vec3 base = direction.normalize();
        if (base.lengthSqr() < 1.0E-8D) {
            return direction;
        }

        // 以 base 为轴构造一组正交基（base 接近竖直时换一个参考向量避免退化）
        Vec3 reference = Math.abs(base.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : TargetBox.UP;
        Vec3 axisA = base.cross(reference).normalize();
        Vec3 axisB = base.cross(axisA).normalize();

        double angle = Math.toRadians(random.nextDouble() * MAX_DEFLECTION_DEGREES);
        double roll = random.nextDouble() * Math.PI * 2.0D;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        return base.scale(cos)
            .add(axisA.scale(sin * Math.cos(roll)))
            .add(axisB.scale(sin * Math.sin(roll)))
            .normalize()
            .scale(direction.length());
    }

    /** {@return 该伤害是否属于物理伤害（弹射物已在情况 1 单独处理）} */
    private static boolean isPhysical(@Nullable DamageSource source) {
        return source != null && DamageCategory.resolve(source) == DamageCategory.PHYSICS;
    }

    // ==========================================================================================
    // 射线与（未旋转的）AABB
    // ==========================================================================================

    /**
     * 射线与轴对齐包围盒求交，返回第一个交点与进入面的法线（法线朝向盒子外侧）。
     * <p>起点在盒内时返回起点本身，法线取离起点最近的盒面。未相交时返回 {@code isHit() == false}。</p>
     */
    private static RayHit clipRay(AABB box, Vec3 start, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-12D) {
            return RayHit.MISS;
        }
        Vec3 dir = direction.normalize();
        if (dir.lengthSqr() < 1.0E-12D) {
            return RayHit.MISS;
        }

        double tNear = Double.NEGATIVE_INFINITY;
        double tFar = Double.POSITIVE_INFINITY;
        Vec3 nearNormal = null;

        for (int axis = 0; axis < 3; axis++) {
            double d = component(dir, axis);
            double s = component(start, axis);
            double lo = axis == 0 ? box.minX : (axis == 1 ? box.minY : box.minZ);
            double hi = axis == 0 ? box.maxX : (axis == 1 ? box.maxY : box.maxZ);

            if (Math.abs(d) < 1.0E-9D) {
                // 与该轴平行：起点必须落在该轴的范围之内
                if (s < lo || s > hi) {
                    return RayHit.MISS;
                }
                continue;
            }

            double t1 = (lo - s) / d;
            double t2 = (hi - s) / d;
            boolean flipped = t1 > t2;
            if (flipped) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }

            if (t1 > tNear) {
                tNear = t1;
                // d > 0 时在 lo 侧进入（法线朝坐标轴负方向），d < 0 时在 hi 侧进入（法线朝正方向）
                double sign = d > 0.0D ? -1.0D : 1.0D;
                nearNormal = new Vec3(
                    axis == 0 ? sign : 0.0D, axis == 1 ? sign : 0.0D, axis == 2 ? sign : 0.0D);
            }
            if (t2 < tFar) {
                tFar = t2;
            }
            if (tNear > tFar) {
                return RayHit.MISS;
            }
        }

        if (tFar < 0.0D) {
            // 盒子完全在射线起点之后
            return RayHit.MISS;
        }
        if (tNear < 0.0D) {
            // 起点在盒内：直接取起点，法线取最近的面
            return new RayHit(start, nearestNormal(box, start));
        }
        return new RayHit(start.add(dir.scale(tNear)), nearNormal);
    }

    private static double component(Vec3 vec, int axis) {
        return switch (axis) {
            case 0 -> vec.x;
            case 1 -> vec.y;
            default -> vec.z;
        };
    }

    /** 离命中点最近的盒面法线（法线朝外） */
    private static Vec3 nearestNormal(AABB box, Vec3 point) {
        double[] distances = {
            point.x - box.minX, box.maxX - point.x,
            point.y - box.minY, box.maxY - point.y,
            point.z - box.minZ, box.maxZ - point.z
        };
        int nearest = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[nearest]) {
                nearest = i;
            }
        }
        return switch (nearest) {
            case 0 -> new Vec3(-1.0D, 0.0D, 0.0D);
            case 1 -> new Vec3(1.0D, 0.0D, 0.0D);
            case 2 -> new Vec3(0.0D, -1.0D, 0.0D);
            case 3 -> new Vec3(0.0D, 1.0D, 0.0D);
            case 4 -> new Vec3(0.0D, 0.0D, -1.0D);
            default -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    /**
     * 一条候选射线。
     *
     * @param start     起点（世界坐标）
     * @param direction 方向（长度无要求，可为零长度向量表示“无方向”）
     */
    private record Ray(Vec3 start, Vec3 direction) {}

    /**
     * AABB 求交结果。
     *
     * @param point  第一个交点（起点在盒内时为起点），未命中为 null
     * @param normal 进入面的外法线，未命中或无法确定时为 null
     */
    private record RayHit(@Nullable Vec3 point, @Nullable Vec3 normal) {
        static final RayHit MISS = new RayHit(null, null);

        boolean isHit() {
            return this.point != null;
        }
    }
}
