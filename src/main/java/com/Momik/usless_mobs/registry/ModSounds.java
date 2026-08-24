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
    public static final RegistryObject<SoundEvent> POLAR_BEAR_AMBIENT = register("polar_bear_ambient");
    public static final RegistryObject<SoundEvent> POLAR_BEAR_HURT = register("polar_bear_hurt");
    public static final RegistryObject<SoundEvent> POLAR_BEAR_DEATH = register("polar_bear_death");
    public static final RegistryObject<SoundEvent> POLAR_BEAR_CHARGE = register("polar_bear_charge");
    public static final RegistryObject<SoundEvent> LIVING_BAT_AMBIENT = register("living_bat_ambient");
    public static final RegistryObject<SoundEvent> LIVING_BAT_HURT = register("living_bat_hurt");
    public static final RegistryObject<SoundEvent> LIVING_BAT_DEATH = register("living_bat_death");
    public static final RegistryObject<SoundEvent> ROOTED_HUSK_AMBIENT = register("rooted_husk_ambient");
    public static final RegistryObject<SoundEvent> ROOTED_HUSK_HURT = register("rooted_husk_hurt");
    public static final RegistryObject<SoundEvent> ROOTED_HUSK_DEATH = register("rooted_husk_death");
    public static final RegistryObject<SoundEvent> WEB_CAVE_SPIDER_AMBIENT = register("web_cave_spider_ambient");
    public static final RegistryObject<SoundEvent> WEB_CAVE_SPIDER_HURT = register("web_cave_spider_hurt");
    public static final RegistryObject<SoundEvent> WEB_CAVE_SPIDER_DEATH = register("web_cave_spider_death");
    public static final RegistryObject<SoundEvent> WEB_CAVE_SPIDER_CAST = register("web_cave_spider_cast");
    public static final RegistryObject<SoundEvent> FROST_STRAY_AMBIENT = register("frost_stray_ambient");
    public static final RegistryObject<SoundEvent> FROST_STRAY_HURT = register("frost_stray_hurt");
    public static final RegistryObject<SoundEvent> FROST_STRAY_DEATH = register("frost_stray_death");
    public static final RegistryObject<SoundEvent> FROST_STRAY_VOLLEY = register("frost_stray_volley");
    public static final RegistryObject<SoundEvent> CORAL_DROWNED_AMBIENT = register("coral_drowned_ambient");
    public static final RegistryObject<SoundEvent> CORAL_DROWNED_HURT = register("coral_drowned_hurt");
    public static final RegistryObject<SoundEvent> CORAL_DROWNED_DEATH = register("coral_drowned_death");
    public static final RegistryObject<SoundEvent> CORAL_DROWNED_SURGE = register("coral_drowned_surge");

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
