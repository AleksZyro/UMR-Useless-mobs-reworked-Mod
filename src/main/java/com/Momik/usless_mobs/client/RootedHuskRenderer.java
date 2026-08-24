package com.Momik.usless_mobs.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class RootedHuskRenderer extends HuskRenderer {
    public RootedHuskRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.ROOTED_HUSK, CustomMobModelLayers.ROOTED_HUSK_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
