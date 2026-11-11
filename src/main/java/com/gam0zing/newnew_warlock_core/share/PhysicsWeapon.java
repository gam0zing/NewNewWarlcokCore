package com.gam0zing.newnew_warlock_core.share;

import java.util.List;

import com.gam0zing.newnew_warlock_core.body.BodyEffect;
import com.gam0zing.newnew_warlock_core.body.EffectDelta;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 物理武器档案（挂在物品的 {@code physics_weapon} 数据组件上）。
 * <p>武器的"原始伤害"来自本次伤害事件的数值；本档案只决定这份原始伤害如何被拆成肉伤与甲伤。</p>
 *
 * @param penetrationScale 穿透倍率（举例）：决定伤害穿透护甲的能力
 */
public record PhysicsWeapon(
    PhysicsStat lowRigidityScale,
    PhysicsStat midRigidityScale,
    PhysicsStat highRigidityScale,
    PhysicsStat penetrationScale,
    List<EffectDelta> effects) {

    /** 低/中刚性分界（含中档下限） */
    public static final double MID_RIGIDITY_MIN = 0.5D;

    /** 中/高刚性分界（含高档下限） */
    public static final double HIGH_RIGIDITY_MIN = 1.5D;

    /**
     * 默认武器档案：所有刚性档倍率与穿透都是 1，且不无视护甲。
     * <p>本次攻击的物品没有 {@code physics_weapon} 组件时（空手、爆炸、环境伤害等）使用，
     * 保证所有物理伤害仍然走完整的护甲结算，而不是绕过护甲。</p>
     */
    public static final PhysicsWeapon DEFAULT = new PhysicsWeapon(
        PhysicsStat.ONE, PhysicsStat.ONE, PhysicsStat.ONE, PhysicsStat.ONE, List.of());

    public static final Codec<PhysicsWeapon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PhysicsStat.CODEC.optionalFieldOf("low_rigidity_scale", PhysicsStat.ONE).forGetter(PhysicsWeapon::lowRigidityScale),
        PhysicsStat.CODEC.optionalFieldOf("mid_rigidity_scale", PhysicsStat.ONE).forGetter(PhysicsWeapon::midRigidityScale),
        PhysicsStat.CODEC.optionalFieldOf("high_rigidity_scale", PhysicsStat.ONE).forGetter(PhysicsWeapon::highRigidityScale),
        PhysicsStat.CODEC.optionalFieldOf("penetration_scale", PhysicsStat.ONE).forGetter(PhysicsWeapon::penetrationScale),
        EffectDelta.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(PhysicsWeapon::effects))
        .apply(instance, PhysicsWeapon::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsWeapon> STREAM_CODEC =
        StreamCodec.composite(
            PhysicsStat.STREAM_CODEC, PhysicsWeapon::lowRigidityScale,
            PhysicsStat.STREAM_CODEC, PhysicsWeapon::midRigidityScale,
            PhysicsStat.STREAM_CODEC, PhysicsWeapon::highRigidityScale,
            PhysicsStat.STREAM_CODEC, PhysicsWeapon::penetrationScale,
            EffectDelta.STREAM_CODEC.apply(ByteBufCodecs.list()), PhysicsWeapon::effects,
            PhysicsWeapon::new);

    public PhysicsWeapon {
        lowRigidityScale = lowRigidityScale == null ? PhysicsStat.ONE : lowRigidityScale;
        midRigidityScale = midRigidityScale == null ? PhysicsStat.ONE : midRigidityScale;
        highRigidityScale = highRigidityScale == null ? PhysicsStat.ONE : highRigidityScale;
        penetrationScale = penetrationScale == null ? PhysicsStat.ONE : penetrationScale;
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /** {@return 该刚性值对应的档位倍率档案} */
    public PhysicsStat scaleFor(double rigidity) {
        if (rigidity < MID_RIGIDITY_MIN) {
            return this.lowRigidityScale;
        }
        if (rigidity < HIGH_RIGIDITY_MIN) {
            return this.midRigidityScale;
        }
        return this.highRigidityScale;
    }

    /** {@return 该刚性值对应的档位倍率（已求值，非负）} */
    public float scaleValueFor(double rigidity) {
        return (float) Math.max(0.0D, scaleFor(rigidity).value());
    }

    /** {@return 穿透倍率（已求值，非负）} */
    public float penetration() {
        return (float) Math.max(0.0D, this.penetrationScale.value());
    }
}
