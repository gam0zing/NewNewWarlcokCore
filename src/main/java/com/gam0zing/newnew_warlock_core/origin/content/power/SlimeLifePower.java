package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 【史莱姆生命】：所有由最大生命值驱动的特性合并为一项。
 * <p>属性部分在这里实时维护（每 tick 依据当前最大生命重算，因此分裂、巨大化、装备加成都会即时反映）：
 * 体型与攻击力。缩放只写 {@code minecraft:scale}，碰撞箱、命中盒与渲染由原版自动跟随。</p>
 * <p>药水时长、进食/饮用速度、消化速度同样以最大生命值为准，分别由
 * {@code OriginTraits#adjustPotion}、{@code OriginTraits#slimeUseDuration} 与
 * {@code FoodDataSlowRegenMixin} 读取同一套换算。</p>
 */
final class SlimeLifePower extends TraitPower {

    private static final String SIZE_KEY = "slime/size";
    private static final String ATTACK_KEY = "slime/attack";

    @Override
    public void onGranted(ServerPlayer player, NoConfig config) {
        update(player);
    }

    @Override
    public void onLogin(ServerPlayer player, NoConfig config) {
        update(player);
    }

    @Override
    public void onRespawn(ServerPlayer player, NoConfig config) {
        update(player);
    }

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        update(player);
    }

    @Override
    public void onRevoked(ServerPlayer player, NoConfig config) {
        clearModifier(player, Attributes.SCALE, SIZE_KEY);
        clearModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_KEY);
    }

    private static void update(ServerPlayer player) {
        double ratio = Mth.clamp(
            OriginTuning.SLIME_SIZE_BASE + OriginTuning.SLIME_SIZE_PER_HEALTH * player.getMaxHealth(),
            OriginTuning.SLIME_SIZE_MIN,
            OriginTuning.SLIME_SIZE_MAX);
        setModifier(player, Attributes.SCALE, SIZE_KEY, ratio - 1.0D,
            AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_KEY, ratio - 1.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
