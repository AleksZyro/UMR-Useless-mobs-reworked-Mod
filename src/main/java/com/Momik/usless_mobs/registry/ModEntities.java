package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.entity.LivingBossEntity;
import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.entity.SlimeSpikeProjectile;
import com.Momik.usless_mobs.entity.WebCaveSpiderEntity;
import com.Momik.usless_mobs.entity.WitchBossEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.silverfish.CorruptedSilverfishEntity;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Usless_mobs.MODID);

    public static final RegistryObject<EntityType<BlueSlimeEntity>> BLAUER_SCHLEIM = ENTITY_TYPES.register("blauer_schleim",
            () -> EntityType.Builder.of(BlueSlimeEntity::new, MobCategory.MONSTER)
                    .sized(2.04F, 2.04F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":blauer_schleim"));

    public static final RegistryObject<EntityType<KingSlimeEntity>> KING_SCHLEIM = ENTITY_TYPES.register("king_schleim",
            () -> EntityType.Builder.of(KingSlimeEntity::new, MobCategory.MONSTER)
                    .sized(2.04F, 2.04F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(Usless_mobs.MODID + ":king_schleim"));

    public static final RegistryObject<EntityType<SlimeSpikeProjectile>> SLIME_SPIKE = ENTITY_TYPES.register("slime_spike",
            () -> EntityType.Builder.<SlimeSpikeProjectile>of(SlimeSpikeProjectile::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(Usless_mobs.MODID + ":slime_spike"));

    public static final RegistryObject<EntityType<EnderSlimeEntity>> ENDER_SCHLEIM = ENTITY_TYPES.register("ender_schleim",
            () -> EntityType.Builder.of(EnderSlimeEntity::new, MobCategory.MONSTER)
                    .sized(2.04F, 2.04F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":ender_schleim"));

    public static final RegistryObject<EntityType<CelestialSlimeEntity>> CELESTIAL_SLIME = ENTITY_TYPES.register("celestial_slime",
            () -> EntityType.Builder.of(CelestialSlimeEntity::new, MobCategory.MONSTER)
                    .sized(2.04F, 2.04F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(Usless_mobs.MODID + ":celestial_slime"));

    public static final RegistryObject<EntityType<CorruptedSilverfishEntity>> CORRUPTED_SILVERFISH = ENTITY_TYPES.register("corrupted_silverfish",
            () -> EntityType.Builder.of(CorruptedSilverfishEntity::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.3F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":corrupted_silverfish"));

    public static final RegistryObject<EntityType<LivingBossEntity>> LIVING_BOSS = ENTITY_TYPES.register("living_boss",
            () -> EntityType.Builder.of(LivingBossEntity::new, MobCategory.MONSTER)
                    .sized(1.95F, 2.2F)
                    .clientTrackingRange(12)
                    .build(Usless_mobs.MODID + ":living_boss"));

    public static final RegistryObject<EntityType<FrostStrayEntity>> FROST_STRAY = ENTITY_TYPES.register("frost_stray",
            () -> EntityType.Builder.of(FrostStrayEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":frost_stray"));

    public static final RegistryObject<EntityType<WebCaveSpiderEntity>> WEB_CAVE_SPIDER = ENTITY_TYPES.register("web_cave_spider",
            () -> EntityType.Builder.of(WebCaveSpiderEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.5F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":web_cave_spider"));

    public static final RegistryObject<EntityType<CoralDrownedEntity>> CORAL_DROWNED = ENTITY_TYPES.register("coral_drowned",
            () -> EntityType.Builder.of(CoralDrownedEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":coral_drowned"));

    public static final RegistryObject<EntityType<OctopusEntity>> OCTOPUS = ENTITY_TYPES.register("octopus",
            () -> EntityType.Builder.of(OctopusEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":octopus"));

    public static final RegistryObject<EntityType<WitchBossEntity>> WITCH_BOSS = ENTITY_TYPES.register("witch_boss",
            () -> EntityType.Builder.of(WitchBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(Usless_mobs.MODID + ":witch_boss"));
}
