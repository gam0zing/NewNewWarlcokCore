package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.gam0zing.newnew_warlock_core.share.BiomeClimate;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;
import com.gam0zing.newnew_warlock_core.share.SlimeClimateMobEffect;
import com.gam0zing.newnew_warlock_core.share.SlimeDehydratedMobEffect;
import com.gam0zing.newnew_warlock_core.share.SlimeFrozenMobEffect;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 史莱姆：低温 / 高温干燥环境。
 * <p>进入对应群系时施加 1 级环境效果并持续 2 分钟；效果到期时按当时环境升降级。
 * 减速与受伤倍率由这里维护的临时修饰符同步，周期伤害由效果自身结算。</p>
 */
final class BiomeHazardPower extends TraitPower {

    private static final int CHECK_INTERVAL = 20;
    private static final String SPEED_KEY = "slime_climate_speed";
    private static final String HURT_KEY = "slime_climate_hurt";

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        syncModifiers(player);
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        switch (BiomeClimate.at(player)) {
            case COLD -> apply(player, ModMobEffects.SLIME_FROZEN);
            case HOT_DRY -> apply(player, ModMobEffects.SLIME_DEHYDRATED);
            case NORMAL -> { }
        }
    }

    @Override
    public void onRevoked(ServerPlayer player, NoConfig config) {
        clearModifier(player, Attributes.MOVEMENT_SPEED, SPEED_KEY);
        clearModifier(player, ModAttributes.HURT_SCALE, HURT_KEY);
    }

    private static void apply(ServerPlayer player, Holder<MobEffect> effect) {
        if (player.getEffect(effect) != null) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, SlimeClimateMobEffect.durationFor(player), 0,
            false, true, true));
    }

    private static void syncModifiers(ServerPlayer player) {
        float reduction = 0.0F;
        float hurtBonus = 0.0F;
        MobEffectInstance frozen = player.getEffect(ModMobEffects.SLIME_FROZEN);
        if (frozen != null) {
            float value = SlimeFrozenMobEffect.reductionFor(frozen.getAmplifier() + 1);
            reduction += value;
            hurtBonus += value;
        }
        MobEffectInstance dehydrated = player.getEffect(ModMobEffects.SLIME_DEHYDRATED);
        if (dehydrated != null) {
            reduction += SlimeDehydratedMobEffect.reductionFor(dehydrated.getAmplifier() + 1);
        }
        updateModifier(player, Attributes.MOVEMENT_SPEED, SPEED_KEY, -Mth.clamp(reduction, 0.0F, 1.0F));
        updateModifier(player, ModAttributes.HURT_SCALE, HURT_KEY, hurtBonus);
    }

    /** 数值为 0 时移除修饰符，避免残留空的属性条目。 */
    private static void updateModifier(ServerPlayer player, Holder<Attribute> attribute, String key, double amount) {
        if (Math.abs(amount) < 1.0E-6D) {
            clearModifier(player, attribute, key);
            return;
        }
        setModifier(player, attribute, key, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
