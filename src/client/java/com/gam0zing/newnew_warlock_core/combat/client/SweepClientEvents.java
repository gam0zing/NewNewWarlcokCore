package com.gam0zing.newnew_warlock_core.combat.client;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.sweep.SweepEntities;
import com.gam0zing.newnew_warlock_core.combat.sweep.SweepParticles;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** 横扫之波的客户端表现：实体本身不渲染，弧光由服务端发送的粒子勾出。 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class SweepClientEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SweepEntities.SWEEP_WAVE.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SweepParticles.SWEEP_WAVE.get(), SweepWaveParticle.Provider::new);
    }

    private SweepClientEvents() {}
}
