package com.gam0zing.newnew_warlock_core.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;

/**
 * 武器 / 护甲档案上的“受击效果增量”条目。
 *
 * <p>与身体部位的 {@link BodyEffect} 不同，这里的三个数值都是<b>增量</b>，JSON 字段名统一带
 * {@code delta_} 前缀：</p>
 * <ul>
 *     <li>{@code delta_duration}：在部位已有持续时间上增减的刻数</li>
 *     <li>{@code delta_amplifier}：在部位已有等级上增减的等级；身体部位没有该效果时基准等级为 0，
 *     因此 {@code delta_amplifier=1} 恰好得到 1 级（原版 amplifier 0）</li>
 *     <li>{@code delta_probability}：在部位已有概率上增减的概率</li>
 * </ul>
 * <p>合并规则见 {@link BodyEffect#merge}。</p>
 *
 * @param effect      效果（注册名已解析为注册表持有者）
 * @param duration    持续时间增量（缺省 0）
 * @param amplifier   等级增量（缺省 0）
 * @param probability 概率增量（缺省 0）
 */
public record EffectDelta(Holder<MobEffect> effect, int duration, int amplifier, float probability) {

    public static final Codec<EffectDelta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect_id").forGetter(EffectDelta::effect),
        Codec.INT.optionalFieldOf("delta_duration", 0).forGetter(EffectDelta::duration),
        Codec.INT.optionalFieldOf("delta_amplifier", 0).forGetter(EffectDelta::amplifier),
        Codec.FLOAT.optionalFieldOf("delta_probability", 0.0F).forGetter(EffectDelta::probability))
        .apply(instance, EffectDelta::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EffectDelta> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT), EffectDelta::effect,
            ByteBufCodecs.VAR_INT, EffectDelta::duration,
            ByteBufCodecs.VAR_INT, EffectDelta::amplifier,
            ByteBufCodecs.FLOAT, EffectDelta::probability,
            EffectDelta::new);
}
