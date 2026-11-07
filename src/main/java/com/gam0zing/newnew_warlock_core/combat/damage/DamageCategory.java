package com.gam0zing.newnew_warlock_core.combat.damage;

import com.gam0zing.newnew_warlock_core.combat.DamageTags;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * 伤害分类。由 {@link DamageTags} 中的自定义伤害标签解析而来。
 * <p>解析顺序即优先级：先命中的标签生效；均未命中时回退到 {@link #NORMAL}（覆盖其他模组新增的未打标签类型）。</p>
 */
public enum DamageCategory {
    PHYSICS(DamageTags.PHYSICS),
    MAGIC(DamageTags.MAGIC),
    FIRE(DamageTags.FIRE),
    FROST(DamageTags.FROST),
    POISON(DamageTags.POISON),
    WITHER(DamageTags.WITHER),
    ELECTRIC(DamageTags.ELECTRIC),
    NORMAL(DamageTags.NORMAL);

    private static final DamageCategory[] VALUES = values();

    private final TagKey<DamageType> tag;

    DamageCategory(TagKey<DamageType> tag) {
        this.tag = tag;
    }

    public TagKey<DamageType> tag() {
        return this.tag;
    }

    public static DamageCategory resolve(DamageSource source) {
        for (DamageCategory category : VALUES) {
            if (source.is(category.tag)) {
                return category;
            }
        }
        return NORMAL;
    }
}
