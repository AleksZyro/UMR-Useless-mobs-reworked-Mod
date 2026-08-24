package com.Momik.usless_mobs.client;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;

public class LivingBatRenderer extends BatRenderer {
    public LivingBatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.LIVING_BAT, CustomMobModelLayers.LIVING_BAT_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Bat entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
