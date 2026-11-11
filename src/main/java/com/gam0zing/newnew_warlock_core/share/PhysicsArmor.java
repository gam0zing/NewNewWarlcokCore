package com.gam0zing.newnew_warlock_core.share;

import java.util.List;

import com.gam0zing.newnew_warlock_core.body.BodyEffect;
import com.gam0zing.newnew_warlock_core.body.EffectDelta;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * 物理护甲档案（挂在物品的 {@code physics_armor} 数据组件上）。
 * <p>所有数值都是 {@link PhysicsStat}（基准值 + 修正列表），便于实现装备 Build 玩法。</p>
 *
 * @param rigidity 刚性（举例）：决定护甲抗穿透能力与甲伤档位倍率的判据
 */
public record PhysicsArmor(
    Identifier defenceFuncId,
    PhysicsStat thickness,
    PhysicsStat density,
    PhysicsStat toughness,
    PhysicsStat rigidity,
    PhysicsStat cushioning,
    PhysicsStat insulation,
    PhysicsStat flexibility,
    List<EffectDelta> effects) {

    public static final Codec<PhysicsArmor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("defence_func", PhysicsDefenceFunctions.GENERAL)
            .forGetter(PhysicsArmor::defenceFuncId),
        PhysicsStat.CODEC.optionalFieldOf("thickness", PhysicsStat.of(1.0D)).forGetter(PhysicsArmor::thickness),
        PhysicsStat.CODEC.optionalFieldOf("density", PhysicsStat.ZERO).forGetter(PhysicsArmor::density),
        PhysicsStat.CODEC.optionalFieldOf("toughness", PhysicsStat.ZERO).forGetter(PhysicsArmor::toughness),
        PhysicsStat.CODEC.optionalFieldOf("rigidity", PhysicsStat.of(0.5D)).forGetter(PhysicsArmor::rigidity),
        PhysicsStat.CODEC.optionalFieldOf("cushioning", PhysicsStat.ZERO).forGetter(PhysicsArmor::cushioning),
        PhysicsStat.CODEC.optionalFieldOf("insulation", PhysicsStat.ZERO).forGetter(PhysicsArmor::insulation),
        PhysicsStat.CODEC.optionalFieldOf("flexibility", PhysicsStat.ONE).forGetter(PhysicsArmor::flexibility),
        EffectDelta.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(PhysicsArmor::effects))
        .apply(instance, PhysicsArmor::new));

    /** 效果增量列表的收发（用于手写的 {@link #STREAM_CODEC}） */
    private static final StreamCodec<RegistryFriendlyByteBuf, List<EffectDelta>> EFFECTS_STREAM_CODEC =
        EffectDelta.STREAM_CODEC.apply(ByteBufCodecs.list());

    /** 字段数超过 {@code StreamCodec.composite} 上限（6），手写收发 */
    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsArmor> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> {
            Identifier.STREAM_CODEC.encode(buf, value.defenceFuncId());
            PhysicsStat.STREAM_CODEC.encode(buf, value.thickness());
            PhysicsStat.STREAM_CODEC.encode(buf, value.density());
            PhysicsStat.STREAM_CODEC.encode(buf, value.toughness());
            PhysicsStat.STREAM_CODEC.encode(buf, value.rigidity());
            PhysicsStat.STREAM_CODEC.encode(buf, value.cushioning());
            PhysicsStat.STREAM_CODEC.encode(buf, value.insulation());
            PhysicsStat.STREAM_CODEC.encode(buf, value.flexibility());
            EFFECTS_STREAM_CODEC.encode(buf, value.effects());
        },
        buf -> new PhysicsArmor(
            Identifier.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            PhysicsStat.STREAM_CODEC.decode(buf),
            EFFECTS_STREAM_CODEC.decode(buf)));

    public PhysicsArmor {
        defenceFuncId = defenceFuncId == null ? PhysicsDefenceFunctions.GENERAL : defenceFuncId;
        thickness = thickness == null ? PhysicsStat.of(1.0D) : thickness;
        density = density == null ? PhysicsStat.ZERO : density;
        toughness = toughness == null ? PhysicsStat.ZERO : toughness;
        rigidity = rigidity == null ? PhysicsStat.of(0.5D) : rigidity;
        cushioning = cushioning == null ? PhysicsStat.ZERO : cushioning;
        insulation = insulation == null ? PhysicsStat.ZERO : insulation;
        flexibility = flexibility == null ? PhysicsStat.ONE : flexibility;
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /**
     * {@return 指定耐久比例下的防护力}
     * <p>直接由曲线以"耐久比例 + 盔甲值"两个输入算出，输出不必 ≤ 1
     * （默认 {@code general} 曲线为 {@code 盔甲值 × √耐久比例}）。
     * "盔甲值"是物品的原版护甲值（{@code ArmorItem#getDefense()}），不是这里的厚度。</p>
     *
     * @param durabilityRatio 当前耐久比例，0-1
     * @param armorValue      物品的原版盔甲值
     */
    public double protection(double durabilityRatio, double armorValue) {
        return PhysicsDefenceFunctions.apply(this.defenceFuncId, durabilityRatio, armorValue);
    }

    /** {@return 护甲重量（密度 × 厚度，非负）} */
    public double weight() {
        return Math.max(0.0D, this.density.nonNegativeValue() * this.thickness.nonNegativeValue());
    }
}
