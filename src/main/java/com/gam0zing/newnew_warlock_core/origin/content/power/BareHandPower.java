package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 猫：空手时获得额外攻击力。
 * <p>每 tick 根据主手是否为空更新修饰符，只影响空手状态。</p>
 */
final class BareHandPower extends TraitPower {

    private static final String KEY = "cat/bare_hand";

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        if (player.getMainHandItem().isEmpty()) {
            setModifier(player, Attributes.ATTACK_DAMAGE, KEY,
                OriginTuning.CAT_BARE_HAND_DAMAGE,
                AttributeModifier.Operation.ADD_VALUE);
        } else {
            clearModifier(player, Attributes.ATTACK_DAMAGE, KEY);
        }
    }

    @Override
    public void onRevoked(ServerPlayer player, NoConfig config) {
        clearModifier(player, Attributes.ATTACK_DAMAGE, KEY);
    }
}
