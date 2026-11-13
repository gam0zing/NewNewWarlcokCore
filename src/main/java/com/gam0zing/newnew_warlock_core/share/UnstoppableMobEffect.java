package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * 不可阻挡：每级提供 20% 击退抗性。
 * <p>击退抗性通过 {@code KNOCKBACK_RESISTANCE} 上的临时修饰符实现，效果等级 I 对应 +20%，
 * 每级叠加 20%。效果持续期间每 tick 刷新一次，效果结束或被移除时清除修饰符。</p>
 */
public class UnstoppableMobEffect extends MobEffect {

    /** 每级提供的击退抗性 */
    public static final double RESISTANCE_PER_LEVEL = 0.2D;

    private static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "unstoppable_knockback_resistance");

    public UnstoppableMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        // 每 tick 刷新，保证读档后临时修饰符丢失时能立即恢复
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        applyModifier(entity, amplifier);
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        applyModifier(entity, amplifier);
    }

    private static void applyModifier(LivingEntity entity, int amplifier) {
        AttributeInstance instance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (instance == null) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
            MODIFIER_ID,
            RESISTANCE_PER_LEVEL * (amplifier + 1),
            AttributeModifier.Operation.ADD_VALUE));
    }

    /** 清除击退抗性修饰符。 */
    public static void clearModifier(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (instance != null) {
            instance.removeModifier(MODIFIER_ID);
        }
    }

    /** 效果自然结束。 */
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.getEffect().value() instanceof UnstoppableMobEffect) {
            clearModifier(event.getEntity());
        }
    }

    /** 效果被主动移除。 */
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.getEffect().value() instanceof UnstoppableMobEffect) {
            clearModifier(event.getEntity());
        }
    }
}
