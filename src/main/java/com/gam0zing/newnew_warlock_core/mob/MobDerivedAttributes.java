package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 把四维属性投影到原版属性上。
 * <p>四维（{@code newnew_strength} 等）是唯一数据源，原版属性只是投影：每条目标属性使用固定 id 的
 * 永久修饰符，按当前四维值整体重算并替换，因此重复调用不会叠加。</p>
 * <ul>
 *     <li>strength → {@link Attributes#ATTACK_DAMAGE}</li>
 *     <li>toughness → {@link Attributes#MAX_HEALTH}（乘算），以及 {@link Attributes#ARMOR}、{@link Attributes#ARMOR_TOUGHNESS}（加算）</li>
 *     <li>dexterity → {@link Attributes#MOVEMENT_SPEED} 与 {@link Attributes#FLYING_SPEED}</li>
 *     <li>spirituality → 暂无目标属性</li>
 * </ul>
 * <p>灵巧对地面移速与飞行速度使用同一套换算：高于基准每点 +2.5%，低于基准每点 -10%。
 * 飞行速度只对持有该属性的生物（鹦鹉、恶魂等飞行生物）生效，其余生物自动跳过。</p>
 * <p>坚韧对护甲与盔甲韧性使用加算：相对基准每点 +2 护甲、+0.5 盔甲韧性，低于基准时同样按每点扣减。
 * 注意这两条属性受原版区间限制（护甲 0~30、盔甲韧性 0~20），超出部分不生效。</p>
 * <p>修饰方式：四维的百分比类效果使用 {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}，四维的增减按比例作用于最终值，
 * 会同时放大装备与药水等其它来源；坚韧的护甲类效果使用 {@link AttributeModifier.Operation#ADD_VALUE} 的绝对加成。
 * 修饰符为永久修饰符，随实体属性 NBT 持久化，区块重载、跨维度、
 * 重启后由原版自动恢复，不需要额外的加载钩子。</p>
 * <p>重建只在首次加入世界时调用一次；之后四维若被外部修改（例如后续 GUI 回算宠物等重要生物），
 * 需要显式再调用 {@link #sync(LivingEntity)}。</p>
 */
public final class MobDerivedAttributes {

    /** 四维基准值：10 点视为原版水平，不加不减。 */
    private static final double BASELINE = 10.0D;
    /** 低于基准时，每点减少 10%。 */
    private static final double PENALTY_PER_POINT = 0.10D;
    /** 高于基准时，每点增加 2.5%。 */
    private static final double BONUS_PER_POINT = 0.025D;
    /** 坚韧相对基准每点增加的护甲值。 */
    private static final double ARMOR_PER_TOUGHNESS = 2.0D;
    /** 坚韧相对基准每点增加的盔甲韧性。 */
    private static final double ARMOR_TOUGHNESS_PER_TOUGHNESS = 0.5D;

    private static final Identifier ATTACK_DAMAGE_ID = id("rebuild/attack_damage");
    private static final Identifier MAX_HEALTH_ID = id("rebuild/max_health");
    private static final Identifier ARMOR_ID = id("rebuild/armor");
    private static final Identifier ARMOR_TOUGHNESS_ID = id("rebuild/armor_toughness");
    private static final Identifier MOVEMENT_SPEED_ID = id("rebuild/movement_speed");
    private static final Identifier FLYING_SPEED_ID = id("rebuild/flying_speed");

    /** 按当前四维重算并替换全部派生修饰符。 */
    public static void sync(LivingEntity entity) {
        apply(entity, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID, ModAttributes.NEWNEW_STRENGTH);
        apply(entity, Attributes.MAX_HEALTH, MAX_HEALTH_ID, ModAttributes.NEWNEW_TOUGHNESS);
        applyFlat(entity, Attributes.ARMOR, ARMOR_ID, ModAttributes.NEWNEW_TOUGHNESS, ARMOR_PER_TOUGHNESS);
        applyFlat(entity, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_ID,
            ModAttributes.NEWNEW_TOUGHNESS, ARMOR_TOUGHNESS_PER_TOUGHNESS);
        apply(entity, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_ID, ModAttributes.NEWNEW_DEXTERITY);
        apply(entity, Attributes.FLYING_SPEED, FLYING_SPEED_ID, ModAttributes.NEWNEW_DEXTERITY);
    }

    /**
     * 写入单条派生修饰符。
     * <p>目标属性或四维属性不存在时跳过（例如鸡没有 {@link Attributes#ATTACK_DAMAGE}）；
     * 四维等于基准值时移除旧修饰符，避免留下 0 值条目。</p>
     */
    private static void apply(
        LivingEntity entity,
        Holder<Attribute> target,
        Identifier modifierId,
        Holder<Attribute> stat) {
        AttributeInstance targetInstance = entity.getAttribute(target);
        AttributeInstance statInstance = entity.getAttribute(stat);
        if (targetInstance == null || statInstance == null) {
            return;
        }

        double amount = scaleFor(statInstance.getBaseValue());
        if (amount == 0.0D) {
            targetInstance.removeModifier(modifierId);
            return;
        }
        targetInstance.addOrReplacePermanentModifier(
            new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** 四维值到乘算比例的换算：低于基准按 10% 递减，高于基准按 2.5% 递增。 */
    private static double scaleFor(double stat) {
        double delta = stat - BASELINE;
        return delta >= 0.0D ? delta * BONUS_PER_POINT : delta * PENALTY_PER_POINT;
    }

    /**
     * 写入单条“每点固定值”的派生修饰符（{@link AttributeModifier.Operation#ADD_VALUE}）。
     * <p>目标属性或四维属性不存在时跳过；四维等于基准值时移除旧修饰符，避免留下 0 值条目。</p>
     */
    private static void applyFlat(
        LivingEntity entity,
        Holder<Attribute> target,
        Identifier modifierId,
        Holder<Attribute> stat,
        double amountPerPoint) {
        AttributeInstance targetInstance = entity.getAttribute(target);
        AttributeInstance statInstance = entity.getAttribute(stat);
        if (targetInstance == null || statInstance == null) {
            return;
        }

        double amount = (statInstance.getBaseValue() - BASELINE) * amountPerPoint;
        if (amount == 0.0D) {
            targetInstance.removeModifier(modifierId);
            return;
        }
        targetInstance.addOrReplacePermanentModifier(
            new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }

    private MobDerivedAttributes() {}
}
