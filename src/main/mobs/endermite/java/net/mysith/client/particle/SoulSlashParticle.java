package net.mysith.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class SoulSlashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected SoulSlashParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.friction = 0.96F;
        this.gravity = 0.0F;
        this.lifetime = 10;
        this.quadSize = 0.35F + this.random.nextFloat() * 0.2F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);

        // Bleibt full-color (Textur ist schon rot), nur leicht aufhellen
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Update sprite each tick so all 4 frames cycle through
        this.setSpriteFromAge(this.sprites);

        // Fade out only in last 30% of life
        float lifeRatio = (float) this.age / (float) this.lifetime;
        if (lifeRatio > 0.7F) {
            this.alpha = (1.0F - lifeRatio) / 0.3F;
        } else {
            this.alpha = 1.0F;
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new SoulSlashParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
