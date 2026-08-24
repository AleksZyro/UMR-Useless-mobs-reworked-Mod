package net.mysith.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.MySithMod;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MySithMod.MODID);

    public static final RegistryObject<CreativeModeTab> MYSITH_TAB = TABS.register("mysith_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.usless_mobs.void"))
                    .icon(() -> new ItemStack(ModItems.SITH_SCYTHE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SITH_SCYTHE.get());
                        output.accept(ModItems.VOIDBOUND_SCYTHE.get());
                        output.accept(ModItems.SOUL_FRAGMENT.get());
                        output.accept(ModItems.SOUL_CRYSTAL.get());
                        output.accept(ModItems.DARK_CRYSTAL.get());
                        output.accept(ModItems.VOID_SUMMONER.get());
                        output.accept(ModItems.VOID_CRYSTAL.get());
                        output.accept(ModItems.AWAKENED_VOID_CRYSTAL.get());
                        output.accept(ModItems.VOID_CORE.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOID_SCHLEIMBALL.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOID_VITALITY_TEMPLATE.get());
                        output.accept(ModItems.SOUL_CONTAINER.get());
                        output.accept(ModItems.SOUL_CODEX.get());
                        output.accept(ModItems.SOUL_COMPASS.get());
                        output.accept(ModItems.SOUL_ENDERMITE_SPAWN_EGG.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOID_ALTAR_ITEM.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_CHITIN.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.SILVER_DUST.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.INFESTED_STONE_FRAGMENT.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_SHARD.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_CRYSTAL.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.CORRUPTION_RESONATOR.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.SILVER_FLARE.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.SILVER_DUST_BOMB.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.INFESTED_BAIT.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_CRYSTAL_LEGGINGS.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOID_CRYSTAL_HELMET.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOID_SLIME_CORE_SWORD.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_AXE.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_PICKAXE.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_SHOVEL.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_HOE.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_SHIELD.get());
                        output.accept(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_SILVERFISH_SPAWN_EGG.get());
                    })
                    .build());
}
