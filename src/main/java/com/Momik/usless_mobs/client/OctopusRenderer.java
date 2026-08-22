package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.OctopusEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.resources.ResourceLocation;

public class OctopusRenderer extends SquidRenderer<OctopusEntity> {
    public OctopusRenderer(EntityRendererProvider.Context context) {
        super(context, new OctopusModel<>(context.bakeLayer(ModelLayers.SQUID)));
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.OCTOPUS, CustomMobModelLayers.OCTOPUS_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(OctopusEntity entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
