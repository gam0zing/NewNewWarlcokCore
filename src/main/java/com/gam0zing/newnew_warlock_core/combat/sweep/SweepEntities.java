package com.gam0zing.newnew_warlock_core.combat.sweep;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 横扫相关实体注册。 */
public final class SweepEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, NewNewWarlockCore.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SweepWaveEntity>> SWEEP_WAVE =
        ENTITY_TYPES.register("sweep_wave", () -> EntityType.Builder
            .<SweepWaveEntity>of(SweepWaveEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .noSave()
            .noSummon()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "sweep_wave"))));

    private SweepEntities() {}
}
