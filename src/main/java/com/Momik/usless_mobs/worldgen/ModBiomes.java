package com.Momik.usless_mobs.worldgen;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    public static final ResourceKey<Biome> DEEP_OCEAN = register("deep_ocean");
    public static final ResourceKey<Biome> BIG_UNDERWATER_CAVE = register("big_underwater_cave");

    private ModBiomes() {}

    private static ResourceKey<Biome> register(String name) {
        ResourceLocation id = ResourceLocation.tryBuild(Usless_mobs.MODID, name);
        if (id == null) {
            throw new IllegalArgumentException("Invalid UMR biome id: " + name);
        }
        return ResourceKey.create(Registries.BIOME, id);
    }
}
