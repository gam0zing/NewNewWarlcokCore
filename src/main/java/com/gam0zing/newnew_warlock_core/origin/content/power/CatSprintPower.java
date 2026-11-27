package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 【短跑健将】：奔跑速度提升，代价是奔跑时饥饿消耗更高。
 * <p>只改 {@code run_speed} 属性（仅疾跑时生效），不动 {@code MOVEMENT_SPEED} 面板；
 * 额外的饥饿消耗由 {@code ServerPlayerSprintExhaustionMixin} 在疾跑位移结算处追加。</p>
 */
final class CatSprintPower extends TraitPower {

    private static final String SPEED_KEY = "cat/sprint_speed";

    @Override
    public void onGranted(ServerPlayer player, NoConfig config) {
        apply(player);
    }

    @Override
    public void onLogin(ServerPlayer player, NoConfig config) {
        apply(player);
    }

    @Override
    public void onRespawn(ServerPlayer player, NoConfig config) {
        apply(player);
    }

    @Override
    public void onRevoked(ServerPlayer player, NoConfig config) {
        clearModifier(player, ModAttributes.RUN_SPEED, SPEED_KEY);
    }

    private static void apply(ServerPlayer player) {
        setModifier(player, ModAttributes.RUN_SPEED, SPEED_KEY,
            OriginTuning.CAT_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE);
    }
}
