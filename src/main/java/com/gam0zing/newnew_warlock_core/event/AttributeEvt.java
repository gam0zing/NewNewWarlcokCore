package com.gam0zing.newnew_warlock_core.event;

import java.util.Set;

import com.gam0zing.newnew_warlock_core.registry.ModAttributes;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * 属性挂载。
 * <p>把 {@link ModAttributes#ALL} 中的属性（治疗强度、通用/分类伤害强度）加到所有拥有默认属性表的生物
 * （含玩家与各类生物），使原本没有这些属性的实体也能被相应的强度修饰。</p>
 * <p>{@link ModAttributes#NEWNEW_ATTRIBUTES} 中的生物基础属性挂到除傀儡生物外的所有生物（含玩家）；
 * {@link ModAttributes#NEWNEW_MOB_REBUILD} 只挂到普通生物，玩家与傀儡生物均不添加。</p>
 * <p>该事件在 Mod 事件总线触发，由 {@code NewNewWarlockCore} 构造器显式注册监听。</p>
 */
public final class AttributeEvt {

    /**
     * 不持有生物基础属性的实体类型（傀儡生物）。
     * <p>1.21.1 的 {@link EntityType#getBaseClass()} 恒返回 {@link net.minecraft.world.entity.Entity}，
     * 无法据此判断傀儡生物，因此在这里按注册 ID 维护排除集合。</p>
     */
    private static final Set<Identifier> GOLEM_TYPES = Set.of(
        Identifier.withDefaultNamespace("iron_golem"),
        Identifier.withDefaultNamespace("snow_golem"));

    /** 不持有 {@link ModAttributes#NEWNEW_MOB_REBUILD} 的实体类型：玩家、傀儡生物与盔甲架 */
    private static final Set<Identifier> MOB_REBUILD_EXCLUDED_TYPES = Set.of(
        Identifier.withDefaultNamespace("player"),
        Identifier.withDefaultNamespace("iron_golem"),
        Identifier.withDefaultNamespace("snow_golem"),
        Identifier.withDefaultNamespace("armor_stand"));

    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            Identifier id = EntityType.getKey(type);

            for (var attribute : ModAttributes.ALL) {
                addIfMissing(event, type, attribute);
            }

            if (!GOLEM_TYPES.contains(id)) {
                for (var attribute : ModAttributes.NEWNEW_ATTRIBUTES) {
                    addIfMissing(event, type, attribute);
                }
            }

            if (!MOB_REBUILD_EXCLUDED_TYPES.contains(id)) {
                addIfMissing(event, type, ModAttributes.NEWNEW_MOB_REBUILD);
            }
        }
    }

    private static void addIfMissing(
        EntityAttributeModificationEvent event,
        EntityType<? extends LivingEntity> type,
        Holder<Attribute> attribute) {
        if (!event.has(type, attribute)) {
            event.add(type, attribute);
        }
    }

    private AttributeEvt() {}
}
