package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Usless_mobs.MODID);

    public static final RegistryObject<SoundEvent> HELPING_ALLAY_BOND = register("helping_allay_bond");
    public static final RegistryObject<SoundEvent> HELPING_ALLAY_REVEAL = register("helping_allay_reveal");
    public static final RegistryObject<SoundEvent> HELPING_ALLAY_SHIELD = register("helping_allay_shield");
    public static final RegistryObject<SoundEvent> HELPING_ALLAY_HEAL = register("helping_allay_heal");
    public static final RegistryObject<SoundEvent> HELPING_ALLAY_RETURN = register("helping_allay_return");
    public static final RegistryObject<SoundEvent> OCTOPUS_AMBIENT = register("octopus_ambient");
    public static final RegistryObject<SoundEvent> OCTOPUS_INK = register("octopus_ink");
    public static final RegistryObject<SoundEvent> OCTOPUS_GRAB = register("octopus_grab");
    public static final RegistryObject<SoundEvent> OCTOPUS_CAMOUFLAGE = register("octopus_camouflage");
    public static final RegistryObject<SoundEvent> OCTOPUS_SQUEEZE = register("octopus_squeeze");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.tryBuild(Usless_mobs.MODID, name);
        if (id == null) {
            throw new IllegalArgumentException("Invalid UMR sound id: " + name);
        }
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
