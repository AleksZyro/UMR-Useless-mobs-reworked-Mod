package net.mysith.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.entity.ScytheItemEntity;
import net.mysith.entity.SoulEndermite;
import net.mysith.entity.VoidReaperEntity;
import net.mysith.MySithMod;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MySithMod.MODID);

    public static final RegistryObject<EntityType<SoulEndermite>> SOUL_ENDERMITE =
            ENTITIES.register("soul_endermite",
                    () -> EntityType.Builder.of(SoulEndermite::new, MobCategory.MONSTER)
                            .sized(0.4F, 0.3F)
                            // Vorher 8 — zu kurz: Partikel werden weiter broadcastet als das Entity gerendert wird,
                            // daher sah man "rote Funken ohne Mob". 32 matched Warden/Enderman.
                            .clientTrackingRange(32)
                            .build("soul_endermite"));

    public static final RegistryObject<EntityType<ScytheItemEntity>> SCYTHE_ITEM =
            ENTITIES.register("scythe_item",
                    () -> EntityType.Builder.<ScytheItemEntity>of(ScytheItemEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .build("scythe_item"));

    public static final RegistryObject<EntityType<VoidReaperEntity>> VOID_REAPER =
            ENTITIES.register("void_reaper",
                    () -> EntityType.Builder.of(VoidReaperEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.0F)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build("void_reaper"));
}
