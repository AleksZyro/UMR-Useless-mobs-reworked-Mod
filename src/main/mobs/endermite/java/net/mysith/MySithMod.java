package net.mysith;

import com.mojang.logging.LogUtils;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mysith.network.ModNetworking;
import net.mysith.registry.ModCreativeTabs;
import net.mysith.registry.ModEffects;
import net.mysith.registry.ModEnchantments;
import net.mysith.registry.ModEntities;
import net.mysith.registry.ModItems;
import net.mysith.registry.ModLootModifiers;
import net.mysith.registry.ModParticles;
import net.mysith.registry.ModSounds;
import org.slf4j.Logger;

public class MySithMod {
    public static final String MODID = Usless_mobs.MODID;
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean bootstrapped;

    private MySithMod() {
    }

    public static void bootstrap(IEventBus modBus) {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);

        modBus.addListener(MySithMod::commonSetup);
        ModNetworking.register();
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("MySith mod loaded");
    }
}
