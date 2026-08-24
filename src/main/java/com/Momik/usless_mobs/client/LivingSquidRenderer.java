package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.LivingSquidEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders only the dedicated UMR squid with its exact textured Tripo mesh. */
public class LivingSquidRenderer extends MobRenderer<LivingSquidEntity, SquidModel<LivingSquidEntity>> {
    public LivingSquidRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 0.75F);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.SQUID, CustomMobModelLayers.SQUID_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingSquidEntity squid) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
