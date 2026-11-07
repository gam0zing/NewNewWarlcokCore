package com.gam0zing.newnew_warlock_core.registry;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组属性注册表。
 * <p>属性本身只负责数值容器与同步；属性挂到哪些生物由 {@code event.AttributeEvt} 处理。</p>
 * <p>数值约定：所有强度类属性的基础值都是 1.0，区间 [0, 1024]，直接以倍率形式参与运算；</p>
 * <p>生物基础属性是生物自身的面板数值，与强度倍率无关。</p>
 */
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(Registries.ATTRIBUTE, NewNewWarlockCore.MOD_ID);

    /**
     * 治疗强度：修饰治疗量的倍率，由治疗提供者（{@code healByLiving} 的来源生物）持有。
     * <p>最终治疗量 = 基础治疗量 × 治疗强度。</p>
     */
    public static final DeferredHolder<Attribute, Attribute> HEALING_SCALE = registerScale("healing_scale");

    // ============================== 伤害强度 ==============================
    // 全部是“造成伤害”的属性，因此由攻击者（DamageSource#getEntity）持有并生效。

    /** 通用伤害强度：修正所有类型的伤害 */
    public static final DeferredHolder<Attribute, Attribute> DAMAGE_SCALE = registerScale("damage_scale");
    /** 物理伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> PHYSICS_DAMAGE_SCALE = registerScale("physics_damage_scale");
    /** 魔法伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> MAGIC_DAMAGE_SCALE = registerScale("magic_damage_scale");
    /** 火焰伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> FIRE_DAMAGE_SCALE = registerScale("fire_damage_scale");
    /** 冰霜伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> FROST_DAMAGE_SCALE = registerScale("frost_damage_scale");
    /** 中毒伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> POISON_DAMAGE_SCALE = registerScale("poison_damage_scale");
    /** 凋零伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> WITHER_DAMAGE_SCALE = registerScale("wither_damage_scale");
    /** 电击伤害强度 */
    public static final DeferredHolder<Attribute, Attribute> ELECTRIC_DAMAGE_SCALE = registerScale("electric_damage_scale");

    /** 需要挂载到所有生物身上的属性（供 {@code AttributeEvt} 遍历） */
    public static final List<DeferredHolder<Attribute, Attribute>> ALL = List.of(
        HEALING_SCALE,
        DAMAGE_SCALE,
        PHYSICS_DAMAGE_SCALE,
        MAGIC_DAMAGE_SCALE,
        FIRE_DAMAGE_SCALE,
        FROST_DAMAGE_SCALE,
        POISON_DAMAGE_SCALE,
        WITHER_DAMAGE_SCALE,
        ELECTRIC_DAMAGE_SCALE);

    // ============================== 生物基础属性 ==============================
    // 生物自身的面板数值，除傀儡生物（铁傀儡、雪傀儡）外的所有生物都会持有，玩家也包含在内。
    // 属性点与四项属性都允许负数，用于后续的“属性点分配到属性”机制。

    /** 属性点：默认 0，区间 [-40, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_POINTS = registerRanged("newnew_points", 0.0D, -40.0D, 32767.0D);
    /** 力量：默认 0，区间 [-10, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_STRENGTH = registerRanged("newnew_strength", 0.0D, -10.0D, 32767.0D);
    /** 坚韧：默认 0，区间 [-10, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_TOUGHNESS = registerRanged("newnew_toughness", 0.0D, -10.0D, 32767.0D);
    /** 灵巧：默认 0，区间 [-10, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_DEXTERITY = registerRanged("newnew_dexterity", 0.0D, -10.0D, 32767.0D);
    /** 灵性：默认 0，区间 [-10, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_SPIRITUALITY = registerRanged("newnew_spirituality", 0.0D, -10.0D, 32767.0D);

    /** 属性点可分配的属性（供后续分配逻辑遍历） */
    public static final List<DeferredHolder<Attribute, Attribute>> NEWNEW_ASSIGNABLE_ATTRIBUTES = List.of(
        NEWNEW_STRENGTH,
        NEWNEW_TOUGHNESS,
        NEWNEW_DEXTERITY,
        NEWNEW_SPIRITUALITY);

    /** 需要挂载到除傀儡生物外的所有生物（含玩家）身上的属性（供 {@code AttributeEvt} 遍历） */
    public static final List<DeferredHolder<Attribute, Attribute>> NEWNEW_ATTRIBUTES = List.of(
        NEWNEW_POINTS,
        NEWNEW_STRENGTH,
        NEWNEW_TOUGHNESS,
        NEWNEW_DEXTERITY,
        NEWNEW_SPIRITUALITY);

    /**
     * 生物重建标记：默认 0，区间 [0, 1]。
     * <p>只有普通生物持有，玩家与傀儡生物（铁傀儡、雪傀儡）均不挂载。</p>
     */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_MOB_REBUILD =
        registerRanged("newnew_mob_rebuild", 0.0D, 0.0D, 1.0D);

    private static DeferredHolder<Attribute, Attribute> registerScale(String name) {
        String descriptionId = "attribute." + NewNewWarlockCore.MOD_ID + "." + name;
        return ATTRIBUTES.register(
            name, () -> new RangedAttribute(descriptionId, 1.0D, 0.0D, 1024.0D).setSyncable(true));
    }

    private static DeferredHolder<Attribute, Attribute> registerRanged(
        String name, double defaultValue, double minValue, double maxValue) {
        String descriptionId = "attribute." + NewNewWarlockCore.MOD_ID + "." + name;
        return ATTRIBUTES.register(
            name, () -> new RangedAttribute(descriptionId, defaultValue, minValue, maxValue).setSyncable(true));
    }

    /**
     * 读取任意强度属性的当前值。
     *
     * @return 属性值；实体为 null 或没有该属性（例如未注册默认属性表的自定义实体）时返回基础值 1.0
     */
    public static float getScale(@Nullable LivingEntity entity, Holder<Attribute> attribute) {
        if (entity == null || !attribute.isBound()) {
            return 1.0F;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 1.0F : (float) instance.getValue();
    }

    private ModAttributes() {}
}
