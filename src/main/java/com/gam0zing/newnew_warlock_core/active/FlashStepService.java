package com.gam0zing.newnew_warlock_core.active;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gam0zing.newnew_warlock_core.active.net.FlashStepPayload;
import com.gam0zing.newnew_warlock_core.compat.bettercombat.BetterCombatGate;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 闪身步的服务端实现。
 * <p>全部判定在服务端完成：存活、非旁观、非乘客、在地面、属性值、饥饿值与冷却。
 * 通过时按玩家朝向把水平速度设为「方向 × 属性值 × cos5°」，并额外叠加「属性值 × sin5°」
 * 的垂直分量，让玩家能够离地。</p>
 * <p>手持带【突进】的矛并处于冲锋状态时，向前闪身步会额外获得
 * {@code 0.5 + 0.5 × 突进等级} 的强度，并播放 BC 原版戳刺的视听反馈。</p>
 */
public final class FlashStepService {

    /** 冷却 1 秒。 */
    private static final int COOLDOWN_TICKS = 20;
    /** 4.0 点消耗度 = 1 点饥饿值，因此 2.0 等效 0.5 点饥饿值。 */
    private static final float EXHAUSTION = 2.0F;
    /** 饥饿值不高于该值时无法释放。 */
    private static final int MIN_FOOD_LEVEL = 6;
    /** 作用力整体上抬 5°。 */
    private static final double LIFT_RADIANS = Math.toRadians(5.0D);
    private static final double LIFT_COS = Math.cos(LIFT_RADIANS);
    private static final double LIFT_SIN = Math.sin(LIFT_RADIANS);

    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    public static void handle(FlashStepPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> tryFlashStep(player, payload.direction()));
        }
    }

    /** 尝试释放一次闪身步。 */
    public static void tryFlashStep(ServerPlayer player, byte direction) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger()) {
            return;
        }
        // 闪身步只能在地面释放
        if (!isGrounded(player)) {
            return;
        }
        int lungeLevel = BetterCombatGate.lungeLevel(player, direction);
        double lungeBonus = lungeLevel > 0 ? 0.5D + 0.5D * lungeLevel : 0.0D;
        double power = ModAttributes.getValue(player, ModAttributes.SK_FLASH_STEP) + lungeBonus;
        if (power <= 0.0D) {
            return;
        }
        // 创造模式不检查饥饿值，也不产生饥饿消耗
        boolean creative = player.isCreative();
        if (!creative && player.getFoodData().getFoodLevel() <= MIN_FOOD_LEVEL) {
            return;
        }
        long now = player.level().getGameTime();
        Long readyAt = COOLDOWNS.get(player.getUUID());
        if (readyAt != null && now < readyAt) {
            return;
        }

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        // 面朝 +Z 时，玩家左手方向为 +X。
        double leftX = Mth.cos(yaw);
        double leftZ = Mth.sin(yaw);

        double x;
        double z;
        switch (direction) {
            case 0 -> {
                x = forwardX;
                z = forwardZ;
            }
            case 1 -> {
                x = -forwardX;
                z = -forwardZ;
            }
            case 2 -> {
                x = leftX;
                z = leftZ;
            }
            case 3 -> {
                x = -leftX;
                z = -leftZ;
            }
            default -> {
                return;
            }
        }

        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
        double horizontal = power * LIFT_COS;
        double vertical = power * LIFT_SIN;
        player.setDeltaMovement(
            x * horizontal,
            player.getDeltaMovement().y + vertical,
            z * horizontal);
        player.syncVelocity = true;
        if (!creative) {
            player.causeFoodExhaustion(EXHAUSTION);
        }
    }

    /**
     * {@return 玩家是否站在地面上}
     * <p>服务端的 {@code onGround} 依赖客户端位移包，落地瞬间可能还停留在上一帧的空中状态，
     * 因此再用脚下 0.1 格的碰撞探测兜底，避免“站在地上却放不出”。</p>
     */
    private static boolean isGrounded(ServerPlayer player) {
        if (player.onGround()) {
            return true;
        }
        return !player.level().noCollision(player, player.getBoundingBox().move(0.0D, -0.1D, 0.0D));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
    }

    private FlashStepService() {}
}
