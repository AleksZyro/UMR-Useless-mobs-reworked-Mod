package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.entity.LivingBossEntity;
import com.Momik.usless_mobs.entity.LivingBatEntity;
import com.Momik.usless_mobs.entity.LivingSquidEntity;
import com.Momik.usless_mobs.entity.LivingGlowSquidEntity;
import com.Momik.usless_mobs.entity.LivingPolarBearEntity;
import com.Momik.usless_mobs.entity.LivingAxolotlEntity;
import com.Momik.usless_mobs.entity.LivingOcelotEntity;
import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.entity.RootedHuskEntity;
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
                    .sized(1.10F, 0.92F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":corrupted_silverfish"));

    public static final RegistryObject<EntityType<LivingBossEntity>> LIVING_BOSS = ENTITY_TYPES.register("living_boss",
            () -> EntityType.Builder.of(LivingBossEntity::new, MobCategory.MONSTER)
                    .sized(3.70F, 2.95F)
                    .clientTrackingRange(12)
                    .build(Usless_mobs.MODID + ":living_boss"));

    public static final RegistryObject<EntityType<FrostStrayEntity>> FROST_STRAY = ENTITY_TYPES.register("frost_stray",
            () -> EntityType.Builder.of(FrostStrayEntity::new, MobCategory.MONSTER)
                    .sized(1.10F, 1.95F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":frost_stray"));

    public static final RegistryObject<EntityType<WebCaveSpiderEntity>> WEB_CAVE_SPIDER = ENTITY_TYPES.register("web_cave_spider",
            () -> EntityType.Builder.of(WebCaveSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.30F, 0.56F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":web_cave_spider"));

    public static final RegistryObject<EntityType<CoralDrownedEntity>> CORAL_DROWNED = ENTITY_TYPES.register("coral_drowned",
            () -> EntityType.Builder.of(CoralDrownedEntity::new, MobCategory.MONSTER)
                    .sized(1.40F, 1.95F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":coral_drowned"));

    public static final RegistryObject<EntityType<OctopusEntity>> OCTOPUS = ENTITY_TYPES.register("octopus",
            () -> EntityType.Builder.of(OctopusEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.50F, 1.40F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":octopus"));

    public static final RegistryObject<EntityType<WitchBossEntity>> WITCH_BOSS = ENTITY_TYPES.register("witch_boss",
            () -> EntityType.Builder.of(WitchBossEntity::new, MobCategory.MONSTER)
                    .sized(1.15F, 1.95F)
                    .clientTrackingRange(10)
                    .build(Usless_mobs.MODID + ":witch_boss"));

    public static final RegistryObject<EntityType<HelpingAllayEntity>> HELPING_ALLAY = ENTITY_TYPES.register("helping_allay",
            () -> EntityType.Builder.of(HelpingAllayEntity::new, MobCategory.CREATURE)
                    .sized(0.95F, 0.90F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(Usless_mobs.MODID + ":helping_allay"));

    public static final RegistryObject<EntityType<LivingSquidEntity>> LIVING_SQUID = ENTITY_TYPES.register("living_squid",
            () -> EntityType.Builder.of(LivingSquidEntity::new, MobCategory.WATER_CREATURE)
                    .sized(2.60F, 1.30F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(Usless_mobs.MODID + ":living_squid"));

    public static final RegistryObject<EntityType<LivingGlowSquidEntity>> LIVING_GLOW_SQUID = ENTITY_TYPES.register("living_glow_squid",
            () -> EntityType.Builder.of(LivingGlowSquidEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.45F, 1.80F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(Usless_mobs.MODID + ":living_glow_squid"));

    public static final RegistryObject<EntityType<LivingPolarBearEntity>> LIVING_POLAR_BEAR = ENTITY_TYPES.register("living_polar_bear",
            () -> EntityType.Builder.of(LivingPolarBearEntity::new, MobCategory.CREATURE)
                    .sized(1.90F, 1.40F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(Usless_mobs.MODID + ":living_polar_bear"));

    public static final RegistryObject<EntityType<LivingAxolotlEntity>> LIVING_AXOLOTL = ENTITY_TYPES.register("living_axolotl",
            () -> EntityType.Builder.of(LivingAxolotlEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.35F, 0.65F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(Usless_mobs.MODID + ":living_axolotl"));

    public static final RegistryObject<EntityType<LivingOcelotEntity>> LIVING_OCELOT = ENTITY_TYPES.register("living_ocelot",
            () -> EntityType.Builder.of(LivingOcelotEntity::new, MobCategory.CREATURE)
                    .sized(1.45F, 0.90F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(Usless_mobs.MODID + ":living_ocelot"));

    public static final RegistryObject<EntityType<LivingBatEntity>> LIVING_BAT = ENTITY_TYPES.register("living_bat",
            () -> EntityType.Builder.of(LivingBatEntity::new, MobCategory.AMBIENT)
                    .sized(0.90F, 0.65F)
                    .clientTrackingRange(8)
                    .build(Usless_mobs.MODID + ":living_bat"));

    public static final RegistryObject<EntityType<RootedHuskEntity>> ROOTED_HUSK = ENTITY_TYPES.register("rooted_husk",
            () -> EntityType.Builder.of(RootedHuskEntity::new, MobCategory.MONSTER)
                    .sized(1.10F, 2.15F)
                    .clientTrackingRange(10)
                    .build(Usless_mobs.MODID + ":rooted_husk"));
}
