package com.gam0zing.newnew_warlock_core.combat.physics;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import com.gam0zing.newnew_warlock_core.body.BodyEffect;
import com.gam0zing.newnew_warlock_core.body.BodyPart;
import com.gam0zing.newnew_warlock_core.body.BodyParts;
import com.gam0zing.newnew_warlock_core.combat.damage.DamageCategory;
import com.gam0zing.newnew_warlock_core.combat.damage.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.hit.HitLocation;
import com.gam0zing.newnew_warlock_core.combat.hit.HitLocationResolver;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 第一类伤害（{@link DamageCategory#PHYSICS}）的业务实现。
 * <p>所有 {@link DamageCategory#PHYSICS} 伤害都会进入物理模型：攻击物品带 {@code physics_weapon} 时使用该档案，
 * 否则使用 {@link PhysicsWeapon#DEFAULT}（所有倍率与穿透均为 1、不无视护甲）。
 * 护甲档案是可选项：部位没有可用的护甲档案时直接承受全额肉伤。</p>
 * <p>核心拆分：</p>
 * <ul>
 *     <li>肉伤 = 原始伤害 × 武器倍率(身体部位刚性) × 部位伤害倍率</li>
 *     <li>甲伤 = 原始伤害 × 武器倍率(护甲刚性)</li>
 *     <li>穿透 = 武器穿透倍率</li>
 * </ul>
 * <p>然后原样交给 {@link PhysicsResolver} 结算，得到直接肉伤、震动肉伤、甲伤吸收量与耐久损耗。</p>
 */
public final class PhysicsDamage {

    private PhysicsDamage() {}

    /**
     * 插槽 A：确定本次物理伤害的武器、受击部位与护甲。
     *
     * @return 物理上下文；不是物理伤害时为 null
     */
    @Nullable
    public static PhysicsContext prepare(DamageContext ctx) {
        if (ctx.getCategory() != DamageCategory.PHYSICS) {
            return null;
        }

        PhysicsWeapon weapon = PhysicsEquipment.weapon(ctx.getSource()).orElse(PhysicsWeapon.DEFAULT);

        LivingEntity target = ctx.getTarget();
        // 同一次命中只解析一次 mobId，供部位查找与重量查询共用
        Identifier mobId = EntityType.getKey(target.getType());
        HitLocation location = HitLocationResolver.resolve(ctx.getSource(), target).orElse(null);
        BodyPart bodyPart = location == null
            ? BodyParts.defaultPart(mobId)
            : BodyParts.find(mobId, location.face(), location.x(), location.y());
        PhysicsEquipment.ArmorLoad load = PhysicsEquipment.armorLoad(bodyPart, target);
        boolean ignoresArmor = ctx.getSource().is(DamageTypeTags.BYPASSES_ARMOR);
        double weight = totalWeight(mobId, load.totalWeight());

        return new PhysicsContext(weapon, bodyPart, load.hitArmor(), ignoresArmor, weight);
    }

    /**
     * 汇总目标的总物理重量：身体重量 + 全身护甲重量。
     * <p>身体重量对运行时生物暂时是静态量（数据文件 / 模板里配置的值）。
     * TODO: 以后可能读取生物属性（例如质量、体型等 Attribute）来修饰这个重量，
     * 因此所有重量汇总都应该集中在这里做，而不是散落到结算代码里。</p>
     *
     * @param mobId       受击生物的注册名（已由调用方解析）
     * @param armorWeight 全身护甲重量之和
     * @return 总重量，传给 {@link PhysicsResolver} 参与传递伤害计算
     */
    private static double totalWeight(Identifier mobId, double armorWeight) {
        return Math.max(0.0D, BodyParts.weight(mobId) + armorWeight);
    }

    /**
     * 插槽 C：主伤害计算。
     *
     * @param damage 进入主计算时的原始伤害（已扣除盾牌格挡）
     * @return 结算后的肉伤
     */
    public static float calculate(DamageContext ctx, float damage) {
        PhysicsContext physics = ctx.getPhysics();
        if (physics == null) {
            return damage;
        }

        PhysicsHitResult result = resolve(physics, damage);
        physics.setResult(result);

        // 部位受伤倍率只放大最终肉伤，不影响护甲耐久与甲伤吸收
        float damageScale = Math.max(0.0F, physics.bodyPart().damageScale());
        ctx.setArmorReduction(armorReductionInMeat(result) * damageScale);
        // DEBUG 临时代码
        System.out.println("part=" + physics.bodyPart().name() + " damage=" + result.meatDamage() * damageScale);

        return result.meatDamage() * damageScale;
    }

    /**
     * 把护甲吸收的甲伤按比例换算成等量的肉伤。
     * <p>{@link PhysicsHitResult#armorVolume()} 是“甲伤口径”，而容器
     * {@code Reduction.ARMOR} 需要的是“最终伤害口径”，因此用
     * {@code 肉伤输入 × 吸收体积 / 甲伤输入} 换算。</p>
     */
    private static float armorReductionInMeat(PhysicsHitResult result) {
        float armorDamageInput = result.armorDamageInput();
        if (armorDamageInput <= 0.0F) {
            return 0.0F;
        }
        float absorbedRatio = Math.min(1.0F, Math.max(0.0F, result.armorVolume() / armorDamageInput));
        return result.meatDamageInput() * absorbedRatio;
    }

    /** 插槽 D：按结算结果扣除护甲耐久（不可破坏装备自动忽略） */
    public static void applyArmorDurability(DamageContext ctx) {
        PhysicsContext physics = ctx.getPhysics();
        if (physics == null) {
            return;
        }
        PhysicsHitResult result = physics.result();
        if (result == null) {
            return;
        }
        PhysicsArmorState armor = result.armor();
        if (armor == null) {
            return;
        }
        armor.applyDurabilityLoss(result.durabilityLoss(), ctx.getTarget());
    }

    /**
     * 落地之后：按实际扣血量折算概率，施加合并后的受击效果。
     * <p>效果列表由“身体部位基础值 + 武器增量 + 护甲增量”合并而成（见 {@link BodyEffect#merge}）。
     * 护甲增量只在受击部位确实有护甲时参与。</p>
     *
     * @param dealtDamage  本次实际从目标身上扣除的生命值（不含伤害吸收承担的部分）
     * @param healthBefore 扣除前的生命值
     */
    public static void applyBodyEffects(DamageContext ctx, float dealtDamage, float healthBefore) {
        PhysicsContext physics = ctx.getPhysics();
        if (physics == null) {
            return;
        }
        PhysicsArmorState armor = physics.armor();
        List<BodyEffect> effects = BodyEffect.merge(
            physics.bodyPart().effects(),
            physics.weapon().effects(),
            armor == null ? List.of() : armor.profile().effects());
        if (effects.isEmpty()) {
            return;
        }
        LivingEntity target = ctx.getTarget();
        float maxHealth = target.getMaxHealth();
        for (BodyEffect effect : effects) {
            if (target.getRandom().nextFloat()
                < effect.probabilityFor(dealtDamage, healthBefore, maxHealth)) {
                target.addEffect(new MobEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
            }
        }
    }

    /** 把原始伤害拆成肉伤/甲伤/穿透后交给 {@link PhysicsResolver} */
    private static PhysicsHitResult resolve(PhysicsContext physics, float damage) {
        BodyPart bodyPart = physics.bodyPart();
        PhysicsWeapon weapon = physics.weapon();

        double meat = (double) damage * weapon.scaleValueFor(bodyPart.rigidity());

        PhysicsArmorState armor = physics.armor();
        double armorRigidity = armor == null ? 0.0D : armor.rigidity();
        double armorDamage = armor == null ? 0.0D : (double) damage * weapon.scaleValueFor(armorRigidity);

        return PhysicsResolver.resolve(
            (float) Math.max(0.0D, meat),
            weapon.penetration(),
            (float) Math.max(0.0D, armorDamage),
            physics.ignoresArmor(),
            physics.weight(),
            armor);
    }
}
