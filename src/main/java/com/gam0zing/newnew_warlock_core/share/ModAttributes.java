package com.gam0zing.newnew_warlock_core.share;

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
 * <p>属性本身只负责数值容器与同步；属性挂到哪些生物由 {@code share.AttributeEvt} 处理。</p>
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

    // ============================== 通用战斗属性 ==============================

    /** 附魔能量倍率：附魔台三档选项的 power 乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> ENCHANT_POWER_SCALE = registerScale("enchant_power_scale");
    /** 经验获取倍率：玩家获得的经验值乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> EXP_RECEIVE_SCALE = registerScale("exp_receive_scale");
    /** 奔跑速度倍率：疾跑状态下的移动速度乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> RUN_SPEED = registerScale("run_speed");
    /** 生物掉落倍率：击杀生物后战利品的掉落次数倍率 */
    public static final DeferredHolder<Attribute, Attribute> LOOTING_SCALE = registerScale("looting_scale");
    /** 受伤倍率：实际扣血时的血量扣除量乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> HURT_SCALE = registerScale("hurt_scale");
    /** 闪避：对魔法、出爆炸外的物理伤害有效，有概率在伤害的早期就避免该次伤害 */
    public static final DeferredHolder<Attribute, Attribute> DODGE = registerRanged("dodge", 0.0D, 0.0D, 1.0D);
    /** 暴击率：整数部分为必暴次数，小数部分为额外一次暴击的概率 */
    public static final DeferredHolder<Attribute, Attribute> CRITICAL = registerRanged("critical", 0.02D, 0.0D, 1024.0D);
    /** 暴击伤害：每层暴击对扣血量的倍率 */
    public static final DeferredHolder<Attribute, Attribute> CRITICAL_DAMAGE = registerRanged("critical_damage", 1.5D, 0.0D, 1024.0D);
    /** 时运追加：任何读取时运等级的地方加上该值，向下取整且最终不小于 0 */
    public static final DeferredHolder<Attribute, Attribute> FORTUNE = registerRanged("fortune", 0.0D, -1024.0D, 1024.0D);
    /** 抢夺追加：任何读取抢夺等级的地方加上该值，向下取整且最终不小于 0 */
    public static final DeferredHolder<Attribute, Attribute> LOOTING = registerRanged("looting", 0.0D, -1024.0D, 1024.0D);
    /** 药水等级追加：施加效果时等级增加该值，向下取整，降到 0 以下则放弃施加 */
    public static final DeferredHolder<Attribute, Attribute> POTION_LEVEL = registerRanged("potion_level", 0.0D, -1024.0D, 1024.0D);
    /** 药水时间倍率：施加效果时持续时间乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> POTION_TIME = registerScale("potion_time");
    /** 药水范围倍率：喷溅/滞留型药水的半径乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> POTION_RANGE = registerScale("potion_range");
    /** 弹射物伤害倍率：弹射物造成伤害时乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> PROJECTILE_DAMAGE_SCALE = registerScale("projectile_damage_scale");
    /** 受到治疗倍率：被治疗时治疗量乘以该值，由被治疗者自己持有 */
    public static final DeferredHolder<Attribute, Attribute> RECEIVE_HEALING_SCALE = registerScale("receive_healing_scale");
    /** 弹射物速度倍率：生物发射弹射物时的初速乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> PROJECTILE_VELOCITY = registerScale("projectile_velocity");
    /** 伤害距离修正：扣血阶段双方每远离 1 格扣血量变化的百分比，相距 8 格为零点 */
    public static final DeferredHolder<Attribute, Attribute> DAMAGE_DISTANCE_FIX = registerRanged("damage_distance_fix", -0.01D, -1024.0D, 1024.0D);
    /** 巨人杀手：目标体积大于来源时，对目标造成该数值等级的流血，未满 1 按概率 */
    public static final DeferredHolder<Attribute, Attribute> GIANT_SLAYER = registerRanged("giant_slayer", 0.0D, 0.0D, 1024.0D);
    /** 侏儒杀手：来源体积大于目标时，对目标造成该数值等级的致命伤，未满 1 按概率 */
    public static final DeferredHolder<Attribute, Attribute> DWARF_SLAYER = registerRanged("dwarf_slayer", 0.0D, 0.0D, 1024.0D);
    /** 无甲杀手：目标没穿任何护甲时伤害乘以该值 */
    public static final DeferredHolder<Attribute, Attribute> UNARMORED_SLAYER = registerScale("unarmored_slayer");
    /** 险地矿工：当地难度 >= 30 时挖掉矿物按该概率使掉落物翻倍，难度 15~30 概率减半，可连续翻倍 */
    public static final DeferredHolder<Attribute, Attribute> PERILOUS_MINER = registerRanged("perilous_miner", 0.0D, 0.0D, 1024.0D);
    /** 探险家：吸收经验球时按险地矿工同款概率使经验翻倍；并按 属性值 × 当前位置难度% 提升移动速度 */
    public static final DeferredHolder<Attribute, Attribute> EXPLORER = registerRanged("explorer", 0.0D, 0.0D, 1024.0D);
    /** 邪恶化：大于 0 生效——受到的一切治疗减半，免疫凋零，中毒转为同等级的生命恢复 */
    public static final DeferredHolder<Attribute, Attribute> CORRUPTION = registerRanged("corruption", 0.0D, 0.0D, 1.0D);
    /** 受伤限制：大于等于 0 生效——单次扣血量不超过 最大生命值 × (1 - 该值) */
    public static final DeferredHolder<Attribute, Attribute> HURT_LIMIT = registerRanged("hurt_limit", 0.0D, 0.0D, 1.0D);
    /** 石肤：扣血阶段扣除的血量减去该数值 */
    public static final DeferredHolder<Attribute, Attribute> ROCK_SKIN = registerRanged("rock_skin", 0.0D, 0.0D, 65536.0D);
    /** 移动治疗：每累积移动 1 格对自己治疗一次该数值，等于 0 不结算 */
    public static final DeferredHolder<Attribute, Attribute> MOVE_HEALING = registerRanged("move_healing", 0.0D, 0.0D, 65535.0D);
    /** 生命偷取：扣血阶段结束后，伤害来源按「实际扣血量 × 该值」回复自身，默认 0 不生效 */
    public static final DeferredHolder<Attribute, Attribute> LIFE_STEALING =
        registerRanged("life_stealing", 0.0D, 0.0D, 1024.0D);

    // ============================== 玩家专属属性 ==============================

    /** 隐蔽：玩家被生物发现的范围 = 原本值 × (1 - 该值)，在隐身、潜行与护甲覆盖等可见性结算之后计算 */
    public static final DeferredHolder<Attribute, Attribute> CONCEALMENT =
        registerRanged("concealment", 0.0D, -1024.0D, 1.0D);

    /** 只挂载到玩家身上的属性（供 {@code AttributeEvt} 遍历） */
    public static final List<DeferredHolder<Attribute, Attribute>> PLAYER_ATTRIBUTES = List.of(
        CONCEALMENT);

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
        ELECTRIC_DAMAGE_SCALE,
        ENCHANT_POWER_SCALE,
        EXP_RECEIVE_SCALE,
        RUN_SPEED,
        LOOTING_SCALE,
        HURT_SCALE,
        DODGE,
        CRITICAL,
        CRITICAL_DAMAGE,
        FORTUNE,
        LOOTING,
        POTION_LEVEL,
        POTION_TIME,
        POTION_RANGE,
        PROJECTILE_DAMAGE_SCALE,
        RECEIVE_HEALING_SCALE,
        PROJECTILE_VELOCITY,
        DAMAGE_DISTANCE_FIX,
        GIANT_SLAYER,
        DWARF_SLAYER,
        UNARMORED_SLAYER,
        PERILOUS_MINER,
        EXPLORER,
        CORRUPTION,
        HURT_LIMIT,
        ROCK_SKIN,
        MOVE_HEALING,
        LIFE_STEALING);

    // ============================== 生物基础属性 ==============================
    // 生物自身的面板数值，除傀儡生物（铁傀儡、雪傀儡）外的所有生物都会持有，玩家也包含在内。
    // 属性点与四项属性都允许负数，用于后续的“属性点分配到属性”机制。

    /** 属性点：默认 0，区间 [0, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_POINTS = registerRanged("newnew_points", 0.0D, 0.0D, 32767.0D);
    /** 力量：默认 0，区间 [0, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_STRENGTH = registerRanged("newnew_strength", 10.0D, 1.0D, 32767.0D);
    /** 坚韧：默认 0，区间 [0, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_TOUGHNESS = registerRanged("newnew_toughness", 10.0D, 1.0D, 32767.0D);
    /** 灵巧：默认 0，区间 [0, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_DEXTERITY = registerRanged("newnew_dexterity", 10.0D, 1.0D, 32767.0D);
    /** 灵性：默认 0，区间 [0, 32767] */
    public static final DeferredHolder<Attribute, Attribute> NEWNEW_SPIRITUALITY = registerRanged("newnew_spirituality", 10.0D, 1.0D, 32767.0D);

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

    /**
     * 读取任意属性的当前值，缺失时返回 0。
     * <p>用于基础值不是 1 的属性（暴击率、时运、抢夺、药水等级等）。</p>
     *
     * @return 属性值；实体为 null 或没有该属性时返回 0
     */
    public static double getValue(@Nullable LivingEntity entity, Holder<Attribute> attribute) {
        if (entity == null || !attribute.isBound()) {
            return 0.0D;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getValue();
    }

    private ModAttributes() {}
}
