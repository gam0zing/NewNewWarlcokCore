package com.gam0zing.newnew_warlock_core.combat;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * 战斗模块自定义伤害标签。
 * 数据文件位于 data/newnew_warlock_core/tags/damage_type/ 下，原版伤害类型按类别划分。
 * 未归入任何分类的伤害类型（包括其他模组新增且未打标签的类型）在业务代码中回退到 {@link #NORMAL}。
 */
public final class DamageTags {
    /** 物理伤害：徒手/武器/投掷物/摔落/挤压/爆炸冲击等机械性伤害 */
    public static final TagKey<DamageType> PHYSICS = create("physics");
    /** 魔法伤害：法术、诅咒、灵魂/音波等超自然伤害 */
    public static final TagKey<DamageType> MAGIC = create("magic");
    /** 火焰伤害：燃烧、岩浆、高温方块等 */
    public static final TagKey<DamageType> FIRE = create("fire");
    /** 冰霜伤害：冻结类伤害 */
    public static final TagKey<DamageType> FROST = create("frost");
    /** 中毒伤害 */
    public static final TagKey<DamageType> POISON = create("poison");
    /** 凋零伤害 */
    public static final TagKey<DamageType> WITHER = create("wither");
    /** 电击伤害：雷击、电流等 */
    public static final TagKey<DamageType> ELECTRIC = create("electric");
    /** 内部伤害：绕过防护与加成流程、直接扣除生命值的伤害（如出血、溺水、饥饿、脱水） */
    public static final TagKey<DamageType> INNER_DAMAGE = create("inner_damage");
    /** 兜底分类：虚空、技术性致死等不属于上述类型的伤害 */
    public static final TagKey<DamageType> NORMAL = create("normal");

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, name));
    }

    private DamageTags() {}
}
