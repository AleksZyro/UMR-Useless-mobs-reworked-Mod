package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.entity.LivingAxolotlEntity;
import com.Momik.usless_mobs.entity.LivingBatEntity;
import com.Momik.usless_mobs.entity.LivingBossEntity;
import com.Momik.usless_mobs.entity.LivingGlowSquidEntity;
import com.Momik.usless_mobs.entity.LivingOcelotEntity;
import com.Momik.usless_mobs.entity.LivingPolarBearEntity;
import com.Momik.usless_mobs.entity.LivingSquidEntity;
import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.entity.RootedHuskEntity;
import com.Momik.usless_mobs.entity.WebCaveSpiderEntity;
import com.Momik.usless_mobs.entity.WitchBossEntity;
import com.Momik.usless_mobs.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.silverfish.CorruptedSilverfishEntity;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CommonModEvents {
    private CommonModEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BLAUER_SCHLEIM.get(), BlueSlimeEntity.createAttributes().build());
        event.put(ModEntities.KING_SCHLEIM.get(), KingSlimeEntity.createAttributes().build());
        event.put(ModEntities.ENDER_SCHLEIM.get(), EnderSlimeEntity.createAttributes().build());
        event.put(ModEntities.CELESTIAL_SLIME.get(), CelestialSlimeEntity.createAttributes().build());
        event.put(ModEntities.CORRUPTED_SILVERFISH.get(), CorruptedSilverfishEntity.createAttributes().build());
        event.put(ModEntities.LIVING_BOSS.get(), LivingBossEntity.createAttributes().build());
        event.put(ModEntities.FROST_STRAY.get(), FrostStrayEntity.createAttributes().build());
        event.put(ModEntities.WEB_CAVE_SPIDER.get(), WebCaveSpiderEntity.createAttributes().build());
        event.put(ModEntities.CORAL_DROWNED.get(), CoralDrownedEntity.createAttributes().build());
        event.put(ModEntities.OCTOPUS.get(), OctopusEntity.createAttributes().build());
        event.put(ModEntities.WITCH_BOSS.get(), WitchBossEntity.createAttributes().build());
        event.put(ModEntities.HELPING_ALLAY.get(), HelpingAllayEntity.createAttributes().build());
        event.put(ModEntities.LIVING_SQUID.get(), LivingSquidEntity.createAttributes().build());
        event.put(ModEntities.LIVING_GLOW_SQUID.get(), LivingGlowSquidEntity.createAttributes().build());
        event.put(ModEntities.LIVING_POLAR_BEAR.get(), LivingPolarBearEntity.createAttributes().build());
        event.put(ModEntities.LIVING_AXOLOTL.get(), LivingAxolotlEntity.createAttributes().build());
        event.put(ModEntities.LIVING_OCELOT.get(), LivingOcelotEntity.createAttributes().build());
        event.put(ModEntities.LIVING_BAT.get(), LivingBatEntity.createAttributes().build());
        event.put(ModEntities.ROOTED_HUSK.get(), RootedHuskEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.BLAUER_SCHLEIM.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlueSlimeEntity::checkBlueSlimeSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.KING_SCHLEIM.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                KingSlimeEntity::checkKingSlimeSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.ENDER_SCHLEIM.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EnderSlimeEntity::checkEnderSlimeSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.CELESTIAL_SLIME.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CelestialSlimeEntity::checkCelestialSlimeSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.CORRUPTED_SILVERFISH.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CorruptedSilverfishEntity::checkCorruptedSilverfishSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.FROST_STRAY.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WEB_CAVE_SPIDER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.CORAL_DROWNED.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CoralDrownedEntity::checkCoralDrownedSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
    }
}
