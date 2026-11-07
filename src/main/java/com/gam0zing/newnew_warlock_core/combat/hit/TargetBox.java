package com.gam0zing.newnew_warlock_core.combat.hit;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 目标生物的“朝向盒”：把生物的轴对齐碰撞箱（AABB）绕竖直轴旋转到它自身朝向后得到的有向盒。
 * <p>为什么要虚拟这个盒子：AABB 不会跟着生物转身，斜向的连线会打在“世界坐标看着对、相对生物却不对”的位置；
 * 而部位的区域是按生物自身的面（上/下/左/右/前/后）定义的，所以求交必须用“跟着转身的盒子”。</p>
 *
 * <p>实现上不真的把 8 个顶点转出来，而是把射线换算到盒子自身的坐标系里，
 * 再对局部坐标做一次 slab 求交：局部坐标轴就是生物的“右({@code a}) / 上({@code b}) / 前({@code c})”，
 * 因此进入面（1-3 个候选面中的一个）直接由 slab 求交给出，无需再枚举面。</p>
 *
 * <p>局部坐标约定：{@code a} 沿 {@link #right()}（生物自己的右手侧，含负值）、{@code b} 沿世界竖直向上、
 * {@code c} 沿 {@link #forward()}（生物面朝的一侧，含负值）；盒子在三个方向上的半宽分别取 AABB 的
 * 水平半宽、竖直半高、水平半深。竖直方向不做旋转，因此 {@code b} 与 AABB 的 y 区间一致。</p>
 *
 * <p>注意：朝向盒与 AABB 的边界并不重合。以方形底面为例，旋转 45° 后朝向盒的四个角会露在 AABB 之外，
 * 而 AABB 的四个角又露在朝向盒之外，因此“命中 AABB”与“命中朝向盒”是两件独立的事。</p>
 */
final class TargetBox {

    /** 世界竖直向上 */
    static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);

    /** 判定“射线与该轴平行”以及浮点比较的容差 */
    private static final double EPSILON = 1.0E-9D;

    private final Vec3 center;
    private final Vec3 right;
    private final Vec3 forward;
    private final double halfRight;
    private final double halfUp;
    private final double halfForward;

    private TargetBox(
        Vec3 center, Vec3 right, Vec3 forward,
        double halfRight, double halfUp, double halfForward) {
        this.center = center;
        this.right = right;
        this.forward = forward;
        this.halfRight = halfRight;
        this.halfUp = halfUp;
        this.halfForward = halfForward;
    }

    /**
     * 用生物当前的碰撞箱与朝向构造朝向盒。
     *
     * @param box          生物的轴对齐碰撞箱
     * @param yawDegrees   生物的身体朝向（{@code Entity#getYRot()}）
     */
    static TargetBox of(AABB box, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        return new TargetBox(
            box.getCenter(), right, forward,
            (box.maxX - box.minX) / 2.0D,
            (box.maxY - box.minY) / 2.0D,
            (box.maxZ - box.minZ) / 2.0D);
    }

    /** {@return 盒子中心（与 AABB 的几何中心相同）} */
    Vec3 center() {
        return this.center;
    }

    /** {@return 生物自身的水平前方（单位向量）} */
    Vec3 forward() {
        return this.forward;
    }

    /** {@return 该面的外法线（朝盒外）} */
    Vec3 normalOf(HitFace face) {
        return switch (face) {
            case TOP -> UP;
            case BOTTOM -> UP.scale(-1.0D);
            case FRONT -> this.forward;
            case BACK -> this.forward.scale(-1.0D);
            case RIGHT -> this.right;
            case LEFT -> this.right.scale(-1.0D);
        };
    }

    /** 盒子在某个方向上的最小投影（{@code axis} 需为单位向量） */
    double minProjection(Vec3 axis) {
        return this.center.dot(axis) - extent(axis);
    }

    /** 盒子在某个方向上的最大投影（{@code axis} 需为单位向量） */
    double maxProjection(Vec3 axis) {
        return this.center.dot(axis) + extent(axis);
    }

    private double extent(Vec3 axis) {
        return Math.abs(axis.dot(this.right)) * this.halfRight
            + Math.abs(axis.y) * this.halfUp
            + Math.abs(axis.dot(this.forward)) * this.halfForward;
    }

    /**
     * 射线与朝向盒求交，返回第一个交点与命中的面。
     *
     * @param start     射线起点（世界坐标）
     * @param direction 射线方向（无需归一化，但不能为零向量）
     * @return 命中结果；未相交时返回 null
     */
    @Nullable
    FaceHit clip(Vec3 start, Vec3 direction) {
        double lengthSqr = direction.lengthSqr();
        if (lengthSqr < 1.0E-12D) {
            return null;
        }
        Vec3 dir = direction.scale(1.0D / Math.sqrt(lengthSqr));

        // 换算到盒子自身的坐标系
        Vec3 offset = start.subtract(this.center);
        double a = offset.dot(this.right);
        double b = offset.y;
        double c = offset.dot(this.forward);
        double da = dir.dot(this.right);
        double db = dir.y;
        double dc = dir.dot(this.forward);

        double[] origin = {a, b, c};
        double[] dirs = {da, db, dc};
        double[] half = {this.halfRight, this.halfUp, this.halfForward};
        // 每条重心的两个面上：进入 -half 那一侧时命中的是“低侧”的面，+half 那一侧则是“高侧”的面
        HitFace[] low = {HitFace.LEFT, HitFace.BOTTOM, HitFace.BACK};
        HitFace[] high = {HitFace.RIGHT, HitFace.TOP, HitFace.FRONT};

        double tNear = Double.NEGATIVE_INFINITY;
        double tFar = Double.POSITIVE_INFINITY;
        HitFace nearFace = null;

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(dirs[axis]) < EPSILON) {
                // 与该轴平行：起点在该轴上的坐标必须落在盒内
                if (origin[axis] < -half[axis] || origin[axis] > half[axis]) {
                    return null;
                }
                continue;
            }

            double t1 = (-half[axis] - origin[axis]) / dirs[axis];
            double t2 = (half[axis] - origin[axis]) / dirs[axis];
            HitFace face = low[axis];
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
                face = high[axis];
            }

            if (t1 > tNear) {
                tNear = t1;
                nearFace = face;
            }
            if (t2 < tFar) {
                tFar = t2;
            }
            if (tNear > tFar) {
                return null;
            }
        }

        if (tFar < 0.0D) {
            // 盒子完全在射线起点之后
            return null;
        }
        if (tNear < 0.0D) {
            // 起点已经在盒内：直接取起点，面取最近的那个
            return new FaceHit(nearestFace(a, b, c), start);
        }
        return new FaceHit(nearFace, start.add(dir.scale(tNear)));
    }

    /** 对盒内一点取最近的面 */
    private HitFace nearestFace(double a, double b, double c) {
        double[] distances = {
            a + this.halfRight, this.halfRight - a,
            b + this.halfUp, this.halfUp - b,
            c + this.halfForward, this.halfForward - c
        };
        int nearest = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[nearest]) {
                nearest = i;
            }
        }
        return switch (nearest) {
            case 0 -> HitFace.LEFT;
            case 1 -> HitFace.RIGHT;
            case 2 -> HitFace.BOTTOM;
            case 3 -> HitFace.TOP;
            case 4 -> HitFace.BACK;
            default -> HitFace.FRONT;
        };
    }

    /**
     * 把命中结果转成 {@link HitLocation}：命中面直接由求交给出，
     * 再用“从体外正对该面看”的坐标系算出面内的 0-1 坐标。
     * <p>约定与 {@link HitLocation} 相同：侧面（左/右/前/后）的“上”是世界竖直向上，
     * 上/下面的“上”是生物自身的前方；屏幕右方 = 视线方向 × 屏幕上方向，其中视线方向为 {@code -normal}。</p>
     */
    HitLocation locationOf(FaceHit hit) {
        HitFace face = hit.face();
        Vec3 normal = normalOf(face);
        Vec3 up = face.isSide() ? UP : this.forward;
        Vec3 right = normal.scale(-1.0D).cross(up).normalize();
        Vec3 point = hit.point();

        float x = normalized(point.dot(right), minProjection(right), maxProjection(right));
        float y = normalized(point.dot(up), minProjection(up), maxProjection(up));
        return new HitLocation(face, x, y);
    }

    private static float normalized(double value, double min, double max) {
        double size = max - min;
        if (size <= 1.0E-9D) {
            return 0.5F;
        }
        return (float) ((value - min) / size);
    }

    /**
     * 命中的面与命中点。
     *
     * @param face  命中的面（生物自身相对面）
     * @param point 命中点（世界坐标；起点落在盒内时就是起点本身）
     */
    record FaceHit(HitFace face, Vec3 point) {}
}
