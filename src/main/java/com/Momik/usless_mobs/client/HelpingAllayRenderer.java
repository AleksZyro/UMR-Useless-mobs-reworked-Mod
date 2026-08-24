package com.Momik.usless_mobs.client;

import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;

public final class HelpingAllayRenderer extends AllayRenderer {
    public HelpingAllayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.clear();
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HelpingAllayExactLayer(this, context.getResourceManager()));
        this.shadowRadius = 0.25F;
    }

    @Override
    public ResourceLocation getTextureLocation(Allay entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
