package com.gam0zing.newnew_warlock_core.combat.spine;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 标签伤害强度。
 * <p>造成伤害的属性，由攻击者（{@link net.minecraft.world.damagesource.DamageSource#getEntity()}）持有：</p>
 * <ul>
 *     <li>{@code damage_scale}：通用伤害强度，修正所有类型的伤害；</li>
 *     <li>{@code <tag>_damage_scale}：对应伤害分类的强度，只修正该分类（{@link DamageCategory#NORMAL} 没有专属属性）。</li>
 * </ul>
 * <p>两者相乘叠加。</p>
 */
public final class DamageScaling {

    private static final Map<DamageCategory, DeferredHolder<Attribute, Attribute>> BY_CATEGORY = Map.of(
        DamageCategory.PHYSICS, ModAttributes.PHYSICS_DAMAGE_SCALE,
        DamageCategory.MAGIC, ModAttributes.MAGIC_DAMAGE_SCALE,
        DamageCategory.FIRE, ModAttributes.FIRE_DAMAGE_SCALE,
        DamageCategory.FROST, ModAttributes.FROST_DAMAGE_SCALE,
        DamageCategory.POISON, ModAttributes.POISON_DAMAGE_SCALE,
        DamageCategory.WITHER, ModAttributes.WITHER_DAMAGE_SCALE,
        DamageCategory.ELECTRIC, ModAttributes.ELECTRIC_DAMAGE_SCALE);

    /**
     * 按攻击者的强度属性修正伤害。
     *
     * @param attacker 攻击者，可为 null（环境伤害等无来源的伤害直接原样返回）
     * @param category 本次伤害的分类
     * @param damage   进入主计算时的伤害
     * @return 修正后的伤害 = 原伤害 × 通用伤害强度 × 分类伤害强度
     */
    public static float apply(@Nullable LivingEntity attacker, DamageCategory category, float damage) {
        if (attacker == null || !(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }

        float scaled = damage * ModAttributes.getScale(attacker, ModAttributes.DAMAGE_SCALE);

        DeferredHolder<Attribute, Attribute> categoryScale = BY_CATEGORY.get(category);
        if (categoryScale != null) {
            scaled *= ModAttributes.getScale(attacker, categoryScale);
        }

        return scaled;
    }

    private DamageScaling() {}
}
