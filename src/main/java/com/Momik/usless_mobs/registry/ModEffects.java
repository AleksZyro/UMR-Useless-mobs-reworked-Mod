package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.effect.ElasticityMobEffect;
import com.Momik.usless_mobs.effect.GoldenFlowMobEffect;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Usless_mobs.MODID);

    public static final RegistryObject<MobEffect> ELASTICITY = MOB_EFFECTS.register("elasticity", ElasticityMobEffect::new);

    public static final RegistryObject<MobEffect> GOLDEN_FLOW = MOB_EFFECTS.register("golden_flow", GoldenFlowMobEffect::new);
}
