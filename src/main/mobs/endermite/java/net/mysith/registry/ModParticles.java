package net.mysith.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.MySithMod;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MySithMod.MODID);

    public static final RegistryObject<SimpleParticleType> SOUL_SLASH =
            PARTICLES.register("soul_slash", () -> new SimpleParticleType(false));
}
