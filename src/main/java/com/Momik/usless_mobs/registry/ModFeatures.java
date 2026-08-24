package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.world.CelestialSpireFeature;
import com.Momik.usless_mobs.world.LivingGroveFeature;
import com.Momik.usless_mobs.world.SlimeCaveFeature;
import com.Momik.usless_mobs.world.VoidShrineFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Usless_mobs.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SLIME_CAVE =
            FEATURES.register("slime_cave",
                    () -> new SlimeCaveFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> VOID_SHRINE =
            FEATURES.register("void_shrine",
                    () -> new VoidShrineFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CELESTIAL_SPIRE =
            FEATURES.register("celestial_spire",
                    () -> new CelestialSpireFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LIVING_GROVE =
            FEATURES.register("living_grove",
                    () -> new LivingGroveFeature(NoneFeatureConfiguration.CODEC));
}
