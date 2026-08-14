package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModPotions {
    private ModPotions() {}

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, Usless_mobs.MODID);

    public static final RegistryObject<Potion> ELASTICITY_POTION = POTIONS.register("elasticity",
            () -> new Potion(new MobEffectInstance(ModEffects.ELASTICITY.get(), 3_600)));

    public static final RegistryObject<Potion> LONG_ELASTICITY_POTION = POTIONS.register("long_elasticity",
            () -> new Potion(new MobEffectInstance(ModEffects.ELASTICITY.get(), 9_600)));

    public static final RegistryObject<Potion> GOLDEN_FLOW_POTION = POTIONS.register("golden_flow",
            () -> new Potion(new MobEffectInstance(ModEffects.GOLDEN_FLOW.get(), 1_800)));

    public static final RegistryObject<Potion> STRONG_GOLDEN_FLOW_POTION = POTIONS.register("strong_golden_flow",
            () -> new Potion(new MobEffectInstance(ModEffects.GOLDEN_FLOW.get(), 900, 1)));
}
