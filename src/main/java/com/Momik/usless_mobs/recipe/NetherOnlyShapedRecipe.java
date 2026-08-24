package com.Momik.usless_mobs.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetherOnlyShapedRecipe extends ShapedRecipe {

    public NetherOnlyShapedRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(id, group, category, width, height, ingredients, result);
    }

    public static NetherOnlyShapedRecipe fromShaped(ShapedRecipe base) {
        return new NetherOnlyShapedRecipe(
            base.getId(),
            base.getGroup(),
            base.category(),
            base.getWidth(),
            base.getHeight(),
            base.getIngredients(),
            base.getResultItem(RegistryAccess.EMPTY)
        );
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (level.dimension() != Level.NETHER) {
            return false;
        }
        return super.matches(container, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return com.Momik.usless_mobs.registry.ModRecipeSerializers.NETHER_ONLY_SHAPED_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<NetherOnlyShapedRecipe> {

        private static final ShapedRecipe.Serializer DELEGATE = (ShapedRecipe.Serializer) RecipeSerializer.SHAPED_RECIPE;

        @Override
        public NetherOnlyShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return NetherOnlyShapedRecipe.fromShaped(DELEGATE.fromJson(id, json));
        }

        @Override
        public NetherOnlyShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return NetherOnlyShapedRecipe.fromShaped(DELEGATE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, NetherOnlyShapedRecipe recipe) {
            DELEGATE.toNetwork(buf, recipe);
        }
    }
}
