package com.Momik.usless_mobs.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

import static terrablender.api.ParameterUtils.Continentalness;
import static terrablender.api.ParameterUtils.Depth;
import static terrablender.api.ParameterUtils.ParameterPointListBuilder;

public final class UmrOceanRegion extends Region {
    public UmrOceanRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(
            Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();

        new ParameterPointListBuilder()
                .continentalness(Continentalness.DEEP_OCEAN)
                .depth(Depth.SURFACE)
                .build()
                .forEach(point -> builder.add(point, ModBiomes.DEEP_OCEAN));

        new ParameterPointListBuilder()
                .continentalness(Continentalness.DEEP_OCEAN)
                .depth(Depth.UNDERGROUND)
                .build()
                .forEach(point -> builder.add(point, ModBiomes.BIG_UNDERWATER_CAVE));

        builder.build().forEach(mapper::accept);
    }
}
