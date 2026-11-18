package com.gam0zing.newnew_warlock_core.combat.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * 横扫之波的蓝色弧光粒子。
 * <p>无初速度、受重力影响、寿命 5 tick；弧线位置由服务端计算，客户端只负责渲染。</p>
 */
public final class SweepWaveParticle extends SingleQuadParticle {

    private static final int LIFETIME = 1;
    private static final float SIZE = 0.14F;

    private SweepWaveParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.lifetime = LIFETIME;
        this.gravity = 1.0F;
        this.friction = 0.98F;
        this.hasPhysics = false;
        this.quadSize = SIZE;
        this.rCol = 0.35F;
        this.gCol = 0.72F;
        this.bCol = 1.0F;
    }

    @Override
    protected Layer getLayer() {
        return Layer.bySprite(this.sprite);
    }

    /** 供 {@code RegisterParticleProvidersEvent#registerSpriteSet} 使用。 */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new SweepWaveParticle(level, x, y, z, this.sprites.get(random));
        }
    }
}
