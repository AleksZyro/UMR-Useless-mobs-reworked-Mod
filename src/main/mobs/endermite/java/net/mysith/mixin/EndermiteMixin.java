package net.mysith.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Endermite.class)
public class EndermiteMixin {

    /**
     * Suppress vanilla portal particle spawning for SoulEndermite instances.
     * Vanilla Endermite.tick() emits PORTAL particles client-side; we replace them
     * with our own crimson dust for SoulEndermite.
     */
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")
    )
    private void mysith$replacePortalParticles(Level level, ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {
        Endermite self = (Endermite) (Object) this;
        if (self instanceof net.mysith.entity.SoulEndermite) {
            net.minecraft.core.particles.DustParticleOptions crimson =
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.55F, 0.0F, 0.05F), 1.0F);
            level.addParticle(crimson, x, y, z, vx * 0.3, vy * 0.3, vz * 0.3);
            return;
        }
        level.addParticle(options, x, y, z, vx, vy, vz);
    }
}
