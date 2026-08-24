package com.Momik.usless_mobs.registry;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

public final class ModBrewingRecipes {
    private ModBrewingRecipes() {
    }

    public static void register() {
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
    }

    private static ItemStack potionStack(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }
}
