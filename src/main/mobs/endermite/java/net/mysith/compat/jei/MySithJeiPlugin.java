package net.mysith.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.mysith.MySithMod;
import net.mysith.registry.ModItems;

/**
 * JEI compatibility: vanilla crafting recipes are auto-discovered; this plugin only
 * adds "info" panels (lore + gameplay hints) for each Reaper's-Path item so a player
 * who finds the item via JEI search gets context without reading the Codex.
 */
@JeiPlugin
public class MySithJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.tryBuild(MySithMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        addInfo(registration, ModItems.SITH_SCYTHE.get(),     "jei.usless_mobs.sith_scythe.info");
        addInfo(registration, ModItems.VOIDBOUND_SCYTHE.get(), "jei.usless_mobs.voidbound_scythe.info");
        addInfo(registration, ModItems.SOUL_FRAGMENT.get(),   "jei.usless_mobs.soul_fragment.info");
        addInfo(registration, ModItems.SOUL_CRYSTAL.get(),    "jei.usless_mobs.soul_crystal.info");
        addInfo(registration, ModItems.DARK_CRYSTAL.get(),    "jei.usless_mobs.dark_crystal.info");
        addInfo(registration, ModItems.CELESTIAL_CRYSTAL.get(), "jei.usless_mobs.celestial_crystal.info");
        addInfo(registration, ModItems.AWAKENED_CELESTIAL_CRYSTAL.get(), "jei.usless_mobs.awakened_celestial_crystal.info");
        addInfo(registration, ModItems.VOID_SUMMONER.get(),   "jei.usless_mobs.void_summoner.info");
        addInfo(registration, ModItems.VOID_CRYSTAL.get(),    "jei.usless_mobs.void_crystal.info");
        addInfo(registration, ModItems.AWAKENED_VOID_CRYSTAL.get(), "jei.usless_mobs.awakened_void_crystal.info");
        addInfo(registration, ModItems.VOID_CORE.get(), "jei.usless_mobs.void_core.info");
        addInfo(registration, ModItems.SOUL_CONTAINER.get(),  "jei.usless_mobs.soul_container.info");
        addInfo(registration, ModItems.SOUL_CODEX.get(),      "jei.usless_mobs.soul_codex.info");
        addInfo(registration, ModItems.SOUL_COMPASS.get(),    "jei.usless_mobs.soul_compass.info");
        addInfo(registration, ModItems.SOUL_ENDERMITE_SPAWN_EGG.get(), "jei.usless_mobs.soul_endermite_spawn_egg.info");
    }

    private static void addInfo(IRecipeRegistration r, Item item, String key) {
        r.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(key));
    }
}
