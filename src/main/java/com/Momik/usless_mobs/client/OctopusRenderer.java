package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.resources.ResourceLocation;

public class OctopusRenderer extends SquidRenderer<OctopusEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/ocean/staged/octopus.png");

    public OctopusRenderer(EntityRendererProvider.Context context) {
        super(context, new OctopusModel<>(context.bakeLayer(ModelLayers.SQUID)));
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.OCTOPUS),
                CustomMob3DModel.Variant.OCTOPUS, CustomMobModelLayers.OCTOPUS_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(OctopusEntity entity) {
        return TEXTURE;
    }
}
