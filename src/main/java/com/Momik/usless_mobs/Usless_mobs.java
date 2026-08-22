package com.Momik.usless_mobs;

import com.mojang.logging.LogUtils;
import com.Momik.usless_mobs.client.BlueSlimeRenderer;
import com.Momik.usless_mobs.client.CelestialSlimeRenderer;
import com.Momik.usless_mobs.client.CoralDrownedRenderer;
import com.Momik.usless_mobs.client.CustomMob3DModel;
import com.Momik.usless_mobs.client.CustomMobModelLayers;
import com.Momik.usless_mobs.client.EnderSlimeRenderer;
import com.Momik.usless_mobs.client.FrostStrayRenderer;
import com.Momik.usless_mobs.client.HelpingAllayRenderer;
import com.Momik.usless_mobs.client.KingSlimeRenderer;
import com.Momik.usless_mobs.client.LivingBatRenderer;
import com.Momik.usless_mobs.client.LivingBossRenderer;
import com.Momik.usless_mobs.client.OctopusRenderer;
import com.Momik.usless_mobs.client.RootedHuskRenderer;
import com.Momik.usless_mobs.client.WebCaveSpiderRenderer;
import com.Momik.usless_mobs.client.WitchBossRenderer;
import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.entity.LivingBossEntity;
import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.entity.WebCaveSpiderEntity;
import com.Momik.usless_mobs.entity.WitchBossEntity;
import com.Momik.usless_mobs.network.ModNetwork;
import com.Momik.usless_mobs.registry.ModBlockEntities;
import com.Momik.usless_mobs.registry.ModBlocks;
import com.Momik.usless_mobs.registry.ModCreativeTabs;
import com.Momik.usless_mobs.registry.ModEffects;
import com.Momik.usless_mobs.registry.ModEntities;
import com.Momik.usless_mobs.registry.ModFeatures;
import com.Momik.usless_mobs.registry.ModItems;
import com.Momik.usless_mobs.registry.ModPotions;
import com.Momik.usless_mobs.registry.ModRecipeSerializers;
import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.mysith.client.CorruptedSilverfishRenderer;
import net.mysith.MySithMod;
import net.mysith.silverfish.CorruptedSilverfishEntity;
import org.slf4j.Logger;

@Mod(Usless_mobs.MODID)
public class Usless_mobs {

    public static final String MODID = "usless_mobs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Usless_mobs(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MySithMod.bootstrap(modEventBus);

        if (ModList.get().isLoaded("curios")) {
            com.Momik.usless_mobs.compat.curios.CuriosCompat.init(modEventBus);
            LOGGER.info("Curios detected — King Slime Crown registered as trinket");
        }

        software.bernie.geckolib.GeckoLib.initialize();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(potionStack(Potions.AWKWARD)),
                    Ingredient.of(ModItems.BLAUER_SCHLEIMBALL.get()),
                    potionStack(ModPotions.ELASTICITY_POTION.get()));
            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(potionStack(ModPotions.ELASTICITY_POTION.get())),
                    Ingredient.of(Items.REDSTONE),
                    potionStack(ModPotions.LONG_ELASTICITY_POTION.get()));
            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(potionStack(Potions.AWKWARD)),
                    Ingredient.of(ModItems.GOLDENER_SCHLEIMBALL.get()),
                    potionStack(ModPotions.GOLDEN_FLOW_POTION.get()));
            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(potionStack(ModPotions.GOLDEN_FLOW_POTION.get())),
                    Ingredient.of(Items.GLOWSTONE_DUST),
                    potionStack(ModPotions.STRONG_GOLDEN_FLOW_POTION.get()));
        });
        LOGGER.info("Registered slime content: entity {}, blocks {}, {}, core {}",
                ModEntities.BLAUER_SCHLEIM.getId(),
                ModBlocks.BLAUER_SCHLEIMBLOCK.getId(),
                ModBlocks.GOLDENER_SCHLEIMBLOCK.getId(),
                ModItems.NETHERITE_SCHLEIMKERN.getId());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BLAUER_SCHLEIMBALL);
            event.accept(ModItems.GOLDENER_SCHLEIMBALL);
            event.accept(ModItems.VOID_SCHLEIMBALL);
            event.accept(ModItems.NATURE_CRYSTAL);
            event.accept(ModItems.LIVING_TISSUE);
            event.accept(ModItems.FROST_CORE);
            event.accept(ModItems.LIVING_CORE);
            event.accept(ModItems.LIVING_CRYSTAL);
            event.accept(ModItems.AWAKENED_LIVING_CRYSTAL);
            event.accept(ModItems.AXOLOTL_GILLS);
            event.accept(ModItems.BAT_WING);
            event.accept(ModItems.SHADOWTOOTH);
            event.accept(ModItems.TENTACLE);
            event.accept(ModItems.GLOW_FLARE);
            event.accept(ModItems.POTION_OF_LIFE);
            event.accept(ModItems.CORAL_SCALE);
            event.accept(ModItems.HELPING_AMETHYST);
            event.accept(ModItems.HELPING_SOUL);
            event.accept(ModItems.VOID_VITALITY_TEMPLATE);
            event.accept(ModItems.CELESTIAL_VITALITY_TEMPLATE);
            event.accept(ModItems.BALANCE_UPGRADE_TEMPLATE);
            event.accept(ModItems.TRUE_VOID_TEMPLATE);
            event.accept(ModItems.TRUE_CELESTIAL_TEMPLATE);
            event.accept(ModItems.TRUE_LIVING_TEMPLATE);
            event.accept(ModItems.BALANCE_CATALYST);
            event.accept(ModItems.CORRUPTED_CHITIN);
            event.accept(ModItems.SILVER_DUST);
            event.accept(ModItems.INFESTED_STONE_FRAGMENT);
            event.accept(ModItems.CORRUPTED_SHARD);
            event.accept(ModItems.CORRUPTED_CRYSTAL);
            event.accept(net.mysith.registry.ModItems.DARK_CRYSTAL.get());
            event.accept(net.mysith.registry.ModItems.CELESTIAL_CRYSTAL.get());
            event.accept(net.mysith.registry.ModItems.AWAKENED_CELESTIAL_CRYSTAL.get());
            event.accept(net.mysith.registry.ModItems.VOID_CRYSTAL.get());
            event.accept(net.mysith.registry.ModItems.AWAKENED_VOID_CRYSTAL.get());
            event.accept(net.mysith.registry.ModItems.VOID_CORE.get());
            event.accept(ModItems.SCHLEIMREAKTOR_SCHMIEDEVORLAGE);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.BLAUER_SCHLEIMBLOCK_ITEM);
            event.accept(ModItems.GOLDENER_SCHLEIMBLOCK_ITEM);
            event.accept(ModItems.KING_SLIME_TROPHY);
            event.accept(ModItems.VOID_ALTAR_ITEM);
            event.accept(ModItems.CELESTIAL_ALTAR_ITEM);
            event.accept(ModItems.LIVING_ALTAR_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.SCHLEIMKERN);
            event.accept(ModItems.NETHERITE_SCHLEIMKERN);
            event.accept(ModItems.SLIME_KOMPASS);
            event.accept(ModItems.GLOWBAIT_FISHING_ROD);
            event.accept(ModItems.AXOLOTL_GILLS);
            event.accept(ModItems.BAT_WING);
            event.accept(ModItems.SHADOWTOOTH);
            event.accept(ModItems.GLOW_FLARE);
            event.accept(ModItems.CORRUPTION_RESONATOR);
            event.accept(ModItems.SILVER_FLARE);
            event.accept(ModItems.SILVER_DUST_BOMB);
            event.accept(ModItems.INFESTED_BAIT);
            event.accept(net.mysith.registry.ModItems.VOID_SUMMONER.get());
            event.accept(net.mysith.registry.ModItems.VOIDBOUND_SCYTHE.get());
            event.accept(net.mysith.registry.ModItems.CELESTIAL_SCYTHE.get());
            event.accept(net.mysith.registry.ModItems.BALANCE_SCYTHE.get());
            event.accept(ModItems.VOIDBOUND_AXE);
            event.accept(ModItems.VOIDBOUND_PICKAXE);
            event.accept(ModItems.VOIDBOUND_SHOVEL);
            event.accept(ModItems.VOIDBOUND_HOE);
            event.accept(ModItems.CELESTIAL_AXE);
            event.accept(ModItems.CELESTIAL_PICKAXE);
            event.accept(ModItems.CELESTIAL_SHOVEL);
            event.accept(ModItems.CELESTIAL_HOE);
            event.accept(ModItems.BALANCE_AXE);
            event.accept(ModItems.BALANCE_PICKAXE);
            event.accept(ModItems.BALANCE_SHOVEL);
            event.accept(ModItems.BALANCE_HOE);
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.SCHLEIMREAKTOR_BRUSTPANZER);
            event.accept(ModItems.CORRUPTED_CRYSTAL_LEGGINGS);
            event.accept(ModItems.VOID_CRYSTAL_HELMET);
            event.accept(ModItems.ARMOR_OF_BALANCE_HELMET);
            event.accept(ModItems.ARMOR_OF_BALANCE_CHESTPLATE);
            event.accept(ModItems.ARMOR_OF_BALANCE_LEGGINGS);
            event.accept(ModItems.ARMOR_OF_BALANCE_BOOTS);
            event.accept(ModItems.KING_SLIME_KRONE);
            event.accept(ModItems.NETHERITE_KINGS_KRONE);
            event.accept(net.mysith.registry.ModItems.VOIDBOUND_SCYTHE.get());
            event.accept(net.mysith.registry.ModItems.CELESTIAL_SCYTHE.get());
            event.accept(net.mysith.registry.ModItems.BALANCE_SCYTHE.get());
            event.accept(ModItems.SCHLEIMKERN_SCHWERT);
            event.accept(ModItems.NETHERITE_SLIME_CORE_SWORD);
            event.accept(ModItems.VOID_SLIME_CORE_SWORD);
            event.accept(ModItems.CELESTIAL_SLIME_CORE_SWORD);
            event.accept(ModItems.BALANCE_SLIME_CORE_SWORD);
            event.accept(ModItems.VOIDBOUND_SHIELD);
            event.accept(ModItems.CELESTIAL_SHIELD);
            event.accept(ModItems.BALANCE_SHIELD);
            event.accept(ModItems.BEAR_CLAW);
            event.accept(ModItems.BEARCLAW_NECKLACE);
            event.accept(ModItems.AWAKENED_BEARCLAW_NECKLACE);
            event.accept(ModItems.ICE_ARROW);
            event.accept(ModItems.LIVING_CRYSTAL_HELMET);
            event.accept(ModItems.LIVING_ROOT_BOOTS);
            event.accept(ModItems.TRUE_CROWN);
            event.accept(ModItems.TRUE_VOID_SWORD);
            event.accept(ModItems.TRUE_CELESTIAL_SWORD);
            event.accept(ModItems.TRUE_LIVING_AXE);
            event.accept(ModItems.VOID_TALISMAN);
            event.accept(ModItems.CELESTIAL_TALISMAN);
            event.accept(ModItems.LIVING_TALISMAN);
            event.accept(ModItems.KING_SLIME_TROPHY);
        }

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.BLAUER_SCHLEIM_SPAWN_EGG);
            event.accept(ModItems.GOLDENER_SCHLEIM_SPAWN_EGG.get().getDefaultInstance());
            event.accept(ModItems.KING_SCHLEIM_SPAWN_EGG);
            event.accept(ModItems.ENDER_SCHLEIM_SPAWN_EGG);
            event.accept(ModItems.CELESTIAL_SLIME_SPAWN_EGG);
            event.accept(ModItems.CORRUPTED_SILVERFISH_SPAWN_EGG);
            event.accept(ModItems.LIVING_BOSS_SPAWN_EGG);
            event.accept(ModItems.FROST_STRAY_SPAWN_EGG);
            event.accept(ModItems.WEB_CAVE_SPIDER_SPAWN_EGG);
            event.accept(ModItems.CORAL_DROWNED_SPAWN_EGG);
            event.accept(ModItems.OCTOPUS_SPAWN_EGG);
            event.accept(ModItems.WITCH_BOSS_SPAWN_EGG);
            event.accept(ModItems.KING_SLIME_SPAWNER);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.Momik.usless_mobs.command.UmrCommand.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

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

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.BLAUER_SCHLEIM.get(), BlueSlimeRenderer::createRenderer);
            event.registerEntityRenderer(ModEntities.KING_SCHLEIM.get(), KingSlimeRenderer::createRenderer);
            event.registerEntityRenderer(ModEntities.ENDER_SCHLEIM.get(), EnderSlimeRenderer::createRenderer);
            event.registerEntityRenderer(ModEntities.CELESTIAL_SLIME.get(), CelestialSlimeRenderer::createRenderer);
            event.registerEntityRenderer(ModEntities.CORRUPTED_SILVERFISH.get(), CorruptedSilverfishRenderer::createRenderer);
            event.registerEntityRenderer(ModEntities.LIVING_BOSS.get(), LivingBossRenderer::new);
            event.registerEntityRenderer(ModEntities.FROST_STRAY.get(), FrostStrayRenderer::new);
            event.registerEntityRenderer(ModEntities.WEB_CAVE_SPIDER.get(), WebCaveSpiderRenderer::new);
            event.registerEntityRenderer(ModEntities.CORAL_DROWNED.get(), CoralDrownedRenderer::new);
            event.registerEntityRenderer(ModEntities.OCTOPUS.get(), OctopusRenderer::new);
            event.registerEntityRenderer(ModEntities.WITCH_BOSS.get(), WitchBossRenderer::new);
            event.registerEntityRenderer(ModEntities.HELPING_ALLAY.get(), HelpingAllayRenderer::new);
            event.registerEntityRenderer(ModEntities.SLIME_SPIKE.get(), ThrownItemRenderer::new);
            event.registerEntityRenderer(EntityType.BAT, LivingBatRenderer::new);
            event.registerEntityRenderer(EntityType.HUSK, RootedHuskRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(CustomMobModelLayers.LIVING_BOSS,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.LIVING_BOSS));
            event.registerLayerDefinition(CustomMobModelLayers.FROST_STRAY,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.FROST_STRAY));
            event.registerLayerDefinition(CustomMobModelLayers.WEB_CAVE_SPIDER,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.WEB_CAVE_SPIDER));
            event.registerLayerDefinition(CustomMobModelLayers.CORAL_DROWNED,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.CORAL_DROWNED));
            event.registerLayerDefinition(CustomMobModelLayers.OCTOPUS,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.OCTOPUS));
            event.registerLayerDefinition(CustomMobModelLayers.WITCH_BOSS,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.WITCH_BOSS));
            event.registerLayerDefinition(CustomMobModelLayers.LIVING_BAT,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.LIVING_BAT));
            event.registerLayerDefinition(CustomMobModelLayers.ROOTED_HUSK,
                    () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.ROOTED_HUSK));
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(com.Momik.usless_mobs.client.ModKeyMappings.TOGGLE_SLIME_EFFECTS);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemProperties.register(ModItems.SLIME_KOMPASS.get(), ResourceLocation.tryBuild(MODID, "slime_kompass_angle"),
                        (stack, level, livingEntity, seed) -> {
                            if (livingEntity == null || stack.getTag() == null
                                    || !stack.getTag().contains("TargetX") || !stack.getTag().contains("TargetZ")) {
                                return 0.0F;
                            }

                            double dx = stack.getTag().getInt("TargetX") + 0.5D - livingEntity.getX();
                            double dz = stack.getTag().getInt("TargetZ") + 0.5D - livingEntity.getZ();
                            if (dx * dx + dz * dz < 0.0001D) {
                                return 0.0F;
                            }

                            double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
                            double relativeAngle = Mth.wrapDegrees(targetYaw - livingEntity.getYRot()) / 360.0D;
                            if (relativeAngle < 0.0D) {
                                relativeAngle += 1.0D;
                            }
                            return (float) relativeAngle;
                        });
            });
        }
    }

    private static ItemStack potionStack(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }
}
