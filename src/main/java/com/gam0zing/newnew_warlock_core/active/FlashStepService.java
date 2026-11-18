package com.gam0zing.newnew_warlock_core.active;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gam0zing.newnew_warlock_core.active.net.FlashStepPayload;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 闪身步的服务端实现。
 * <p>全部判定在服务端完成：存活、非旁观、非乘客、属性值、饥饿值与冷却。
 * 通过时按玩家朝向把水平速度设为「方向 × 属性值」，垂直速度保持原样。</p>
 */
public final class FlashStepService {

    /** 冷却 1 秒。 */
    private static final int COOLDOWN_TICKS = 20;
    /** 4.0 点消耗度 = 1 点饥饿值，因此 2.0 等效 0.5 点饥饿值。 */
    private static final float EXHAUSTION = 2.0F;
    /** 饥饿值不高于该值时无法释放。 */
    private static final int MIN_FOOD_LEVEL = 6;

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
        double power = ModAttributes.getValue(player, ModAttributes.SK_FLASH_STEP);
        if (power <= 0.0D) {
            return;
        }
        if (player.getFoodData().getFoodLevel() <= MIN_FOOD_LEVEL) {
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
        player.setDeltaMovement(x * power, player.getDeltaMovement().y, z * power);
        player.syncVelocity = true;
        player.causeFoodExhaustion(EXHAUSTION);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
    }

    private FlashStepService() {}
}
