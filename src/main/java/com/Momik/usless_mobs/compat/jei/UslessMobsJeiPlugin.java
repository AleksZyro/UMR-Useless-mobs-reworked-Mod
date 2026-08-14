package com.Momik.usless_mobs.compat.jei;

import com.Momik.usless_mobs.recipe.NetherOnlyShapedRecipe;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.List;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class UslessMobsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.tryBuild(Usless_mobs.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<CraftingRecipe> netherOnlyRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(recipe -> recipe instanceof NetherOnlyShapedRecipe)
                .map(recipe -> (CraftingRecipe) recipe)
                .toList();

        registration.addRecipes(RecipeTypes.CRAFTING, netherOnlyRecipes);
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.SCHLEIMKERN.get()),
                Component.translatable("jei.usless_mobs.slime_core_drop"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get()),
                Component.translatable("jei.usless_mobs.king_slime_crown_drop"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_SPAWNER.get()),
                Component.translatable("jei.usless_mobs.nether_only"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_TROPHY.get()),
                Component.translatable("jei.usless_mobs.king_slime_trophy"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_SHARD.get()),
                Component.translatable("jei.usless_mobs.corrupted_shard_drop"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get()),
                Component.translatable("jei.usless_mobs.blue_slime_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.GOLDENER_SCHLEIMBALL.get()),
                Component.translatable("jei.usless_mobs.golden_slime_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.VOID_SCHLEIMBALL.get()),
                Component.translatable("jei.usless_mobs.void_slime_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.ENDER_SCHLEIM_SPAWN_EGG.get()),
                Component.translatable("jei.usless_mobs.ender_slime_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_SLIME_SPAWN_EGG.get()),
                Component.translatable("jei.usless_mobs.celestial_slime_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.SCHLEIMREAKTOR_BRUSTPANZER.get()),
                Component.translatable("jei.usless_mobs.slime_reactor_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.NETHERITE_KINGS_KRONE.get()),
                Component.translatable("jei.usless_mobs.netherite_crown_lore"));
        registration.addItemStackInfo(new ItemStack(com.Momik.usless_mobs.registry.ModItems.LORE_TOME.get()),
                Component.translatable("jei.usless_mobs.lore_tome.info"));
    }
}
