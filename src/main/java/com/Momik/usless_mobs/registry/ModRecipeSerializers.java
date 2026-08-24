package com.Momik.usless_mobs.registry;

import com.Momik.usless_mobs.recipe.NetherOnlyShapedRecipe;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    private ModRecipeSerializers() {}

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Usless_mobs.MODID);

    public static final RegistryObject<RecipeSerializer<NetherOnlyShapedRecipe>> NETHER_ONLY_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("nether_only_shaped", NetherOnlyShapedRecipe.Serializer::new);
}
