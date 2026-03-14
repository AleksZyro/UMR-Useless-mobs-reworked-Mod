package com.Momik.usless_mobs;

import com.Momik.usless_mobs.block.BlueSlimeBlock;
import com.Momik.usless_mobs.block.GoldenSlimeBlock;
import com.Momik.usless_mobs.client.BlueSlimeRenderer;
import com.Momik.usless_mobs.effect.ElasticityMobEffect;
import com.Momik.usless_mobs.effect.GoldenFlowMobEffect;
import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import com.Momik.usless_mobs.item.NetheriteSlimeCoreItem;
import com.Momik.usless_mobs.item.SlimeReactorChestplateItem;
import com.Momik.usless_mobs.item.SlimeCoreItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Usless_mobs.MODID)
public class Usless_mobs {

    public static final String MODID = "usless_mobs";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, MODID);
    public static final RegistryObject<EntityType<BlueSlimeEntity>> BLAUER_SCHLEIM = ENTITY_TYPES.register("blauer_schleim",
            () -> EntityType.Builder.of(BlueSlimeEntity::new, MobCategory.MONSTER)
                    .sized(1.04F, 1.04F)
                    .clientTrackingRange(8)
                    .build(MODID + ":blauer_schleim"));
    public static final RegistryObject<Block> BLAUER_SCHLEIMBLOCK = BLOCKS.register("blauer_schleimblock",
            () -> new BlueSlimeBlock(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(0.2F).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Block> GOLDENER_SCHLEIMBLOCK = BLOCKS.register("goldener_schleimblock",
            () -> new GoldenSlimeBlock(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(0.35F).sound(SoundType.SLIME_BLOCK).lightLevel(state -> 7)));
    public static final RegistryObject<Item> BLAUER_SCHLEIMBALL = ITEMS.register("blauer_schleimball",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GOLDENER_SCHLEIMBALL = ITEMS.register("goldener_schleimball", () -> new Item(new Item.Properties()) {
        @Override
        public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
            return true;
        }
    });
    public static final RegistryObject<Item> SCHLEIMKERN = ITEMS.register("schleimkern",
            () -> new SlimeCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> NETHERITE_SCHLEIMKERN = ITEMS.register("netherite_schleimkern",
            () -> new NetheriteSlimeCoreItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SCHLEIMREAKTOR_SCHMIEDEVORLAGE = ITEMS.register("schleimreaktor_schmiedevorlage",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SCHLEIMREAKTOR_BRUSTPANZER = ITEMS.register("schleimreaktor_brustpanzer",
            () -> new SlimeReactorChestplateItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BLAUER_SCHLEIMBLOCK_ITEM = registerBlockItem("blauer_schleimblock", BLAUER_SCHLEIMBLOCK, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> GOLDENER_SCHLEIMBLOCK_ITEM = registerBlockItem("goldener_schleimblock", GOLDENER_SCHLEIMBLOCK, new Item.Properties().rarity(Rarity.RARE));
    public static final RegistryObject<Item> BLAUER_SCHLEIM_SPAWN_EGG = ITEMS.register("blauer_schleim_spawn_egg",
            () -> new ForgeSpawnEggItem(BLAUER_SCHLEIM, 0x3D7DFF, 0xA7D3FF, new Item.Properties()));
    public static final RegistryObject<MobEffect> ELASTICITY = MOB_EFFECTS.register("elasticity", ElasticityMobEffect::new);
    public static final RegistryObject<MobEffect> GOLDEN_FLOW = MOB_EFFECTS.register("golden_flow", GoldenFlowMobEffect::new);
    public static final RegistryObject<Potion> ELASTICITY_POTION = POTIONS.register("elasticity",
            () -> new Potion(new MobEffectInstance(ELASTICITY.get(), 3_600)));
    public static final RegistryObject<Potion> LONG_ELASTICITY_POTION = POTIONS.register("long_elasticity",
            () -> new Potion(new MobEffectInstance(ELASTICITY.get(), 9_600)));
    public static final RegistryObject<Potion> GOLDEN_FLOW_POTION = POTIONS.register("golden_flow",
            () -> new Potion(new MobEffectInstance(GOLDEN_FLOW.get(), 1_800)));
    public static final RegistryObject<Potion> STRONG_GOLDEN_FLOW_POTION = POTIONS.register("strong_golden_flow",
            () -> new Potion(new MobEffectInstance(GOLDEN_FLOW.get(), 900, 1)));

    public Usless_mobs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        ENTITY_TYPES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(Ingredient.of(potionStack(Potions.AWKWARD)), Ingredient.of(BLAUER_SCHLEIMBALL.get()), potionStack(ELASTICITY_POTION.get()));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(potionStack(ELASTICITY_POTION.get())), Ingredient.of(Items.REDSTONE), potionStack(LONG_ELASTICITY_POTION.get()));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(potionStack(Potions.AWKWARD)), Ingredient.of(GOLDENER_SCHLEIMBALL.get()), potionStack(GOLDEN_FLOW_POTION.get()));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(potionStack(GOLDEN_FLOW_POTION.get())), Ingredient.of(Items.GLOWSTONE_DUST), potionStack(STRONG_GOLDEN_FLOW_POTION.get()));
        });
        LOGGER.info("Registered slime content: entity {}, blocks {}, {}, core {}", BLAUER_SCHLEIM.getId(), BLAUER_SCHLEIMBLOCK.getId(), GOLDENER_SCHLEIMBLOCK.getId(), NETHERITE_SCHLEIMKERN.getId());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(BLAUER_SCHLEIMBALL);
            event.accept(GOLDENER_SCHLEIMBALL);
            event.accept(SCHLEIMREAKTOR_SCHMIEDEVORLAGE);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(BLAUER_SCHLEIMBLOCK_ITEM);
            event.accept(GOLDENER_SCHLEIMBLOCK_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SCHLEIMKERN);
            event.accept(NETHERITE_SCHLEIMKERN);
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SCHLEIMREAKTOR_BRUSTPANZER);
        }

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BLAUER_SCHLEIM_SPAWN_EGG);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(BLAUER_SCHLEIM.get(), BlueSlimeEntity.createAttributes().build());
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
            event.register(BLAUER_SCHLEIM.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlueSlimeEntity::checkBlueSlimeSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.OR);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(BLAUER_SCHLEIM.get(), BlueSlimeRenderer::createRenderer);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(BLAUER_SCHLEIMBLOCK.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(GOLDENER_SCHLEIMBLOCK.get(), RenderType.translucent());
            });
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    private static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block, Item.Properties properties) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), properties));
    }

    private static ItemStack potionStack(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }
}
