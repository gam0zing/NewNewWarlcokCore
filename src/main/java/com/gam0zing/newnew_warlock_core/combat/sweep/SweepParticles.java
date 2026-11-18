package com.gam0zing.newnew_warlock_core.combat.sweep;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 横扫之波的专用粒子。 */
public final class SweepParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, NewNewWarlockCore.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SWEEP_WAVE =
        PARTICLE_TYPES.register("sweep_wave", () -> new SimpleParticleType(false));

    private SweepParticles() {}
}
