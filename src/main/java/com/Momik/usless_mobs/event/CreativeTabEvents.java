package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {
    private CreativeTabEvents() {
    }

    @SubscribeEvent
    public static void addContents(BuildCreativeModeTabContentsEvent event) {
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
            event.accept(ModItems.HELPING_ALLAY_SPAWN_EGG);
            event.accept(ModItems.LIVING_SQUID_SPAWN_EGG);
            event.accept(ModItems.LIVING_GLOW_SQUID_SPAWN_EGG);
            event.accept(ModItems.LIVING_POLAR_BEAR_SPAWN_EGG);
            event.accept(ModItems.LIVING_AXOLOTL_SPAWN_EGG);
            event.accept(ModItems.LIVING_OCELOT_SPAWN_EGG);
            event.accept(ModItems.LIVING_BAT_SPAWN_EGG);
            event.accept(ModItems.ROOTED_HUSK_SPAWN_EGG);
            event.accept(ModItems.KING_SLIME_SPAWNER);
        }
    }
}
