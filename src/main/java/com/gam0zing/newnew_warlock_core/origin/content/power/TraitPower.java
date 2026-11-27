package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.cyberday1.neoorigins.api.power.PowerType;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 无配置代码能力的基类。
 * <p>统一提供 {@link NoConfig} 编解码器与幂等的属性修饰符读写。</p>
 */
abstract class TraitPower extends PowerType<NoConfig> {

    @Override
    public final Codec<NoConfig> codec() {
        return NoConfig.CODEC;
    }

    /** 施加/更新一个固定 id 的修饰符；重复调用不会叠加。 */
    protected static void setModifier(
        ServerPlayer player,
        Holder<Attribute> attribute,
        String key,
        double amount,
        AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(modifierId(key), amount, operation));
    }

    /** 移除固定 id 的修饰符。 */
    protected static void clearModifier(ServerPlayer player, Holder<Attribute> attribute, String key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(modifierId(key));
        }
    }

    protected static Identifier modifierId(String key) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "origin/" + key);
    }
}
