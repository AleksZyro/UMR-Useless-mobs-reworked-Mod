package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.LivingGlowSquidEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders only the dedicated UMR glow squid with its exact 4K Tripo mesh. */
public final class LivingGlowSquidRenderer extends MobRenderer<LivingGlowSquidEntity, SquidModel<LivingGlowSquidEntity>> {
    public LivingGlowSquidRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 0.80F);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.GLOW_SQUID, CustomMobModelLayers.GLOW_SQUID_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingGlowSquidEntity squid) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
