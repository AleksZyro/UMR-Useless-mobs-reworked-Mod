package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Usless_mobs.MODID);

    public static final RegistryObject<CreativeModeTab> USLESS_MOBS_TAB = CREATIVE_TABS.register("usless_mobs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.usless_mobs"))
                    .icon(() -> new ItemStack(ModItems.KING_SLIME_KRONE.get()))
                    .displayItems((params, output) -> {
                        // Slime and Celestial ingredients
                        output.accept(ModItems.BLAUER_SCHLEIMBALL.get());
                        output.accept(ModItems.GOLDENER_SCHLEIMBALL.get());
                        output.accept(ModItems.SCHLEIMREAKTOR_SCHMIEDEVORLAGE.get());
                        output.accept(net.mysith.registry.ModItems.CELESTIAL_CRYSTAL.get());
                        output.accept(net.mysith.registry.ModItems.AWAKENED_CELESTIAL_CRYSTAL.get());
                        output.accept(ModItems.HELPING_AMETHYST.get());
                        output.accept(ModItems.HELPING_SOUL.get());
                        output.accept(ModItems.CELESTIAL_VITALITY_TEMPLATE.get());
                        output.accept(ModItems.BALANCE_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.TRUE_VOID_TEMPLATE.get());
                        output.accept(ModItems.TRUE_CELESTIAL_TEMPLATE.get());
                        output.accept(ModItems.TRUE_LIVING_TEMPLATE.get());
                        output.accept(ModItems.BALANCE_CATALYST.get());
                        // Slime cores
                        output.accept(ModItems.SCHLEIMKERN.get());
                        output.accept(ModItems.NETHERITE_SCHLEIMKERN.get());
                        // Slime and Celestial tools/utilities
                        output.accept(ModItems.SLIME_KOMPASS.get());
                        output.accept(net.mysith.registry.ModItems.CELESTIAL_SCYTHE.get());
                        output.accept(net.mysith.registry.ModItems.BALANCE_SCYTHE.get());
                        output.accept(ModItems.CELESTIAL_AXE.get());
                        output.accept(ModItems.CELESTIAL_PICKAXE.get());
                        output.accept(ModItems.CELESTIAL_SHOVEL.get());
                        output.accept(ModItems.CELESTIAL_HOE.get());
                        output.accept(ModItems.BALANCE_AXE.get());
                        output.accept(ModItems.BALANCE_PICKAXE.get());
                        output.accept(ModItems.BALANCE_SHOVEL.get());
                        output.accept(ModItems.BALANCE_HOE.get());
                        // Slime and Celestial combat/armor
                        output.accept(ModItems.SCHLEIMKERN_SCHWERT.get());
                        output.accept(ModItems.SCHLEIMREAKTOR_BRUSTPANZER.get());
                        output.accept(ModItems.KING_SLIME_KRONE.get());
                        output.accept(ModItems.NETHERITE_KINGS_KRONE.get());
                        output.accept(ModItems.NETHERITE_SLIME_CORE_SWORD.get());
                        output.accept(ModItems.CELESTIAL_SLIME_CORE_SWORD.get());
                        output.accept(ModItems.BALANCE_SLIME_CORE_SWORD.get());
                        output.accept(ModItems.CELESTIAL_SHIELD.get());
                        output.accept(ModItems.BALANCE_SHIELD.get());
                        output.accept(ModItems.ARMOR_OF_BALANCE_HELMET.get());
                        output.accept(ModItems.ARMOR_OF_BALANCE_CHESTPLATE.get());
                        output.accept(ModItems.ARMOR_OF_BALANCE_LEGGINGS.get());
                        output.accept(ModItems.ARMOR_OF_BALANCE_BOOTS.get());
                        output.accept(ModItems.TRUE_CROWN.get());
                        output.accept(ModItems.LORE_TOME.get());
                        // Slime spawner/trophy
                        output.accept(ModItems.KING_SLIME_SPAWNER.get());
                        output.accept(ModItems.KING_SLIME_TROPHY.get());
                        // Slime and Celestial blocks
                        output.accept(ModItems.BLAUER_SCHLEIMBLOCK_ITEM.get());
                        output.accept(ModItems.GOLDENER_SCHLEIMBLOCK_ITEM.get());
                        output.accept(ModItems.CELESTIAL_ALTAR_ITEM.get());
                        // Neue Kristall-Blöcke
                        output.accept(ModItems.LIVING_CRYSTAL_BLOCK_ITEM.get());
                        output.accept(ModItems.VOID_FRAGMENT_BLOCK_ITEM.get());
                        output.accept(ModItems.CELESTIAL_AETHER_BLOCK_ITEM.get());
                        // Slime and Celestial spawn eggs
                        output.accept(ModItems.BLAUER_SCHLEIM_SPAWN_EGG.get());
                        output.accept(ModItems.GOLDENER_SCHLEIM_SPAWN_EGG.get().getDefaultInstance());
                        output.accept(ModItems.KING_SCHLEIM_SPAWN_EGG.get());
                        output.accept(ModItems.ENDER_SCHLEIM_SPAWN_EGG.get());
                        output.accept(ModItems.CELESTIAL_SLIME_SPAWN_EGG.get());
                        output.accept(ModItems.HELPING_ALLAY_SPAWN_EGG.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> LIVING_TAB = CREATIVE_TABS.register("living_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.usless_mobs.living"))
                    .icon(() -> new ItemStack(ModItems.NATURE_CRYSTAL.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.NATURE_CRYSTAL.get());
                        output.accept(ModItems.LIVING_TISSUE.get());
                        output.accept(ModItems.FROST_CORE.get());
                        output.accept(ModItems.LIVING_CORE.get());
                        output.accept(ModItems.LIVING_CRYSTAL.get());
                        output.accept(ModItems.AWAKENED_LIVING_CRYSTAL.get());
                        output.accept(ModItems.LIVING_ALTAR_ITEM.get());
                        output.accept(ModItems.AXOLOTL_GILLS.get());
                        output.accept(ModItems.BAT_WING.get());
                        output.accept(ModItems.SHADOWTOOTH.get());
                        output.accept(ModItems.TENTACLE.get());
                        output.accept(ModItems.GLOW_FLARE.get());
                        output.accept(ModItems.POTION_OF_LIFE.get());
                        output.accept(ModItems.CORAL_SCALE.get());
                        output.accept(ModItems.BEAR_CLAW.get());
                        output.accept(ModItems.BEARCLAW_NECKLACE.get());
                        output.accept(ModItems.AWAKENED_BEARCLAW_NECKLACE.get());
                        output.accept(ModItems.GLOWBAIT_FISHING_ROD.get());
                        output.accept(ModItems.ICE_ARROW.get());
                        output.accept(ModItems.LIVING_CRYSTAL_HELMET.get());
                        output.accept(ModItems.LIVING_ROOT_BOOTS.get());
                        // Endgame Nature gear (parallel zu Celestial/Sith): Sense, Tools, True-Living-Armor, Krone
                        output.accept(net.mysith.registry.ModItems.LIVING_SCYTHE.get());
                        output.accept(ModItems.LIVING_AXE.get());
                        output.accept(ModItems.LIVING_PICKAXE.get());
                        output.accept(ModItems.LIVING_SHOVEL.get());
                        output.accept(ModItems.LIVING_HOE.get());
                        output.accept(ModItems.LIVING_SHIELD.get());
                        output.accept(ModItems.TRUE_LIVING_HELMET.get());
                        output.accept(ModItems.TRUE_LIVING_CHESTPLATE.get());
                        output.accept(ModItems.TRUE_LIVING_LEGGINGS.get());
                        output.accept(ModItems.TRUE_LIVING_BOOTS.get());
                        output.accept(ModItems.LIVING_KING.get());
                        output.accept(ModItems.LIVING_BOSS_SPAWN_EGG.get());
                        output.accept(ModItems.CORRUPTED_SILVERFISH_SPAWN_EGG.get());
                        output.accept(ModItems.FROST_STRAY_SPAWN_EGG.get());
                        output.accept(ModItems.WEB_CAVE_SPIDER_SPAWN_EGG.get());
                        output.accept(ModItems.CORAL_DROWNED_SPAWN_EGG.get());
                        output.accept(ModItems.OCTOPUS_SPAWN_EGG.get());
                        output.accept(ModItems.WITCH_BOSS_SPAWN_EGG.get());
                        output.accept(ModItems.LIVING_SQUID_SPAWN_EGG.get());
                        output.accept(ModItems.GIANT_SQUID_SPAWN_EGG.get());
                        output.accept(ModItems.LIVING_GLOW_SQUID_SPAWN_EGG.get());
                        output.accept(ModItems.LIVING_POLAR_BEAR_SPAWN_EGG.get());
                        output.accept(ModItems.LIVING_AXOLOTL_SPAWN_EGG.get());
                        output.accept(ModItems.LIVING_OCELOT_SPAWN_EGG.get());
                        output.accept(ModItems.LIVING_BAT_SPAWN_EGG.get());
                        output.accept(ModItems.ROOTED_HUSK_SPAWN_EGG.get());
                    })
                    .build());
}
