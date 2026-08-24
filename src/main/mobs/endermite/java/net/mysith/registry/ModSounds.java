package net.mysith.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.MySithMod;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MySithMod.MODID);

    public static final RegistryObject<SoundEvent> SCYTHE_SWING_HEAVY = register("scythe_swing_heavy");
    public static final RegistryObject<SoundEvent> SCYTHE_HIT = register("scythe_hit");
    public static final RegistryObject<SoundEvent> SCYTHE_WHIRLWIND = register("scythe_whirlwind");
    public static final RegistryObject<SoundEvent> SCYTHE_AIR_SLASH = register("scythe_air_slash");
    public static final RegistryObject<SoundEvent> SCYTHE_CHARGED = register("scythe_charged");
    public static final RegistryObject<SoundEvent> SCYTHE_BIND = register("scythe_bind");

    public static final RegistryObject<SoundEvent> CODEX_OPEN = register("codex_open");
    public static final RegistryObject<SoundEvent> CODEX_PAGE = register("codex_page");

    public static final RegistryObject<SoundEvent> REJECTION_THUNDER = register("rejection_thunder");
    public static final RegistryObject<SoundEvent> REJECTION_VOICE = register("rejection_voice");

    public static final RegistryObject<SoundEvent> SOUL_DRAIN_HEAL = register("soul_drain_heal");
    public static final RegistryObject<SoundEvent> SOUL_ENDERMITE_SCREECH = register("soul_endermite_screech");

    public static final RegistryObject<SoundEvent> CORRUPTED_SILVERFISH_AMBIENT = register("corrupted_silverfish_ambient");
    public static final RegistryObject<SoundEvent> CORRUPTED_SILVERFISH_HURT = register("corrupted_silverfish_hurt");
    public static final RegistryObject<SoundEvent> CORRUPTED_SILVERFISH_ATTACK = register("corrupted_silverfish_attack");
    public static final RegistryObject<SoundEvent> CORRUPTED_SILVERFISH_ESCAPE = register("corrupted_silverfish_escape");
    public static final RegistryObject<SoundEvent> CORRUPTED_SILVERFISH_DEATH = register("corrupted_silverfish_death");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(MySithMod.MODID, name)));
    }
}
