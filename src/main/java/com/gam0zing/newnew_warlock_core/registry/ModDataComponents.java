package com.gam0zing.newnew_warlock_core.registry;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsArmor;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsWeapon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品数据组件注册表。
 * <p>{@code physics_weapon} 挂在武器物品上，{@code physics_armor} 挂在护甲物品上；
 * 均同时提供持久化与网络同步 codec（否则客户端物品栈会丢失组件）。</p>
 * <p>原版/其他模组的物品可以在数据包用 {@code minecraft:set_components} 写入，
 * 或由 {@code event.PhysicsDefaultComponents} 在注册期为特定物品挂默认档案。</p>
 */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NewNewWarlockCore.MOD_ID);

    /** 物理武器档案：攻击时读取攻击者武器物品上的该组件 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PhysicsWeapon>> PHYSICS_WEAPON =
        DATA_COMPONENTS.registerComponentType("physics_weapon",
            builder -> builder
                .persistent(PhysicsWeapon.CODEC)
                .networkSynchronized(PhysicsWeapon.STREAM_CODEC));

    /** 物理护甲档案：受击时读取对应装备槽物品上的该组件 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PhysicsArmor>> PHYSICS_ARMOR =
        DATA_COMPONENTS.registerComponentType("physics_armor",
            builder -> builder
                .persistent(PhysicsArmor.CODEC)
                .networkSynchronized(PhysicsArmor.STREAM_CODEC));

    private ModDataComponents() {}
}
