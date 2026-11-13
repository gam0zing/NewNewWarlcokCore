package com.gam0zing.newnew_warlock_core.mob.skill.impl;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.skill.CooldownMobSkill;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 火箭跳跃：价值 20，冷却 20 秒。
 * <p>触发条件：存在仇恨目标，且距离超过 4 格并低于当前等级的距离上限
 * （{@code 4 + 4 × 等级}，即 1 级为 8 格）。</p>
 * <p>触发后以水平面 60° 仰角朝目标起跳，落点为起跳前目标位置周围 3×3 格内的随机点。
 * 初速度按生物的空中物理（重力 0.08、水平阻力 0.91、垂直阻力 0.98）反解抛物线得到。</p>
 */
public final class RocketJumpSkill extends CooldownMobSkill {

    private static final int COOLDOWN_TICKS = 400;
    private static final double LAUNCH_ANGLE_DEGREES = 60.0D;
    private static final double MIN_DISTANCE = 4.0D;
    private static final double BASE_MAX_DISTANCE = 4.0D;
    private static final double MAX_DISTANCE_PER_LEVEL = 4.0D;

    /** 生物空中物理的近似常量 */
    private static final double GRAVITY = 0.08D;
    private static final double HORIZONTAL_DRAG = 0.91D;
    private static final double VERTICAL_DRAG = 0.98D;
    private static final int MAX_SIMULATION_TICKS = 200;
    private static final double MAX_LAUNCH_SPEED = 8.0D;
    private static final int SOLVER_ITERATIONS = 40;

    public RocketJumpSkill() {
        super(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "rocket_jump"), 20.0F, COOLDOWN_TICKS);
    }

    @Override
    public boolean getTrigger(LivingEntity self, int level) {
        if (!(self instanceof Mob mob)) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = Math.sqrt(self.distanceToSqr(target));
        return distance > MIN_DISTANCE && distance < maxDistance(level);
    }

    @Override
    public void invoke(LivingEntity self, int level) {
        if (!(self instanceof Mob mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }

        // 目的地取起跳前目标位置周围 3×3 格内的随机点
        Vec3 destination = target.position().add(
            self.getRandom().nextDouble() * 2.0D - 1.0D,
            0.0D,
            self.getRandom().nextDouble() * 2.0D - 1.0D);

        double dx = destination.x - self.getX();
        double dz = destination.z - self.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4D) {
            return;
        }

        double speed = solveLaunchSpeed(horizontal, destination.y - self.getY());
        double angle = Math.toRadians(LAUNCH_ANGLE_DEGREES);
        double horizontalSpeed = speed * Math.cos(angle);
        double verticalSpeed = speed * Math.sin(angle);

        self.setDeltaMovement(
            dx / horizontal * horizontalSpeed,
            verticalSpeed,
            dz / horizontal * horizontalSpeed);
        // 起跳瞬间按空中阻力推进，避免上一 tick 的落地摩擦吃掉水平速度
        self.setOnGround(false);
    }

    @Override
    public void onCooldown(LivingEntity self, int level) {
        // 冷却期间不需要额外逻辑
    }

    /** {@return 当前等级允许触发跳跃的最大距离} */
    private static double maxDistance(int level) {
        return BASE_MAX_DISTANCE + MAX_DISTANCE_PER_LEVEL * level;
    }

    /**
     * 在固定仰角下反解初速度，使抛物线落点尽量接近目标水平距离。
     *
     * @param horizontalDistance 起跳点到目的地的水平距离
     * @param heightDifference   目的地相对起跳点的高度差
     */
    private static double solveLaunchSpeed(double horizontalDistance, double heightDifference) {
        double low = 0.0D;
        double high = MAX_LAUNCH_SPEED;
        while (simulateHorizontalRange(high, heightDifference) < horizontalDistance
            && high < MAX_LAUNCH_SPEED * 4.0D) {
            high *= 2.0D;
        }
        for (int iteration = 0; iteration < SOLVER_ITERATIONS; iteration++) {
            double mid = (low + high) * 0.5D;
            if (simulateHorizontalRange(mid, heightDifference) < horizontalDistance) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) * 0.5D;
    }

    /** 模拟一次固定仰角起跳，返回下降到目标高度时的水平位移。 */
    private static double simulateHorizontalRange(double speed, double targetHeight) {
        double angle = Math.toRadians(LAUNCH_ANGLE_DEGREES);
        double horizontalVelocity = speed * Math.cos(angle);
        double verticalVelocity = speed * Math.sin(angle);
        double x = 0.0D;
        double y = 0.0D;

        for (int tick = 0; tick < MAX_SIMULATION_TICKS; tick++) {
            double nextX = x + horizontalVelocity;
            double nextY = y + verticalVelocity;

            // 只在下落阶段判定落点，避免起跳初期的上升段误判
            if (verticalVelocity < 0.0D && y > targetHeight && nextY <= targetHeight) {
                double progress = (y - targetHeight) / (y - nextY);
                return x + (nextX - x) * progress;
            }

            x = nextX;
            y = nextY;
            verticalVelocity = (verticalVelocity - GRAVITY) * VERTICAL_DRAG;
            horizontalVelocity *= HORIZONTAL_DRAG;
        }
        return x;
    }
}
