package com.Momik.usless_mobs;

import com.mojang.logging.LogUtils;
import com.Momik.usless_mobs.network.ModNetwork;
import com.Momik.usless_mobs.registry.ModBlockEntities;
import com.Momik.usless_mobs.registry.ModBlocks;
import com.Momik.usless_mobs.registry.ModBrewingRecipes;
import com.Momik.usless_mobs.registry.ModCreativeTabs;
import com.Momik.usless_mobs.registry.ModEffects;
import com.Momik.usless_mobs.registry.ModEntities;
import com.Momik.usless_mobs.registry.ModFeatures;
import com.Momik.usless_mobs.registry.ModItems;
import com.Momik.usless_mobs.registry.ModPotions;
import com.Momik.usless_mobs.registry.ModRecipeSerializers;
import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.mysith.MySithMod;
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
            ModBrewingRecipes.register();
        });
        LOGGER.info("Registered slime content: entity {}, blocks {}, {}, core {}",
                ModEntities.BLAUER_SCHLEIM.getId(),
                ModBlocks.BLAUER_SCHLEIMBLOCK.getId(),
                ModBlocks.GOLDENER_SCHLEIMBLOCK.getId(),
                ModItems.NETHERITE_SCHLEIMKERN.getId());
    }

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.Momik.usless_mobs.command.UmrCommand.register(event.getDispatcher());
    }

}
