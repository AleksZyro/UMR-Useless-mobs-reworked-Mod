package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.LivingPolarBearEntity;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders only the dedicated UMR polar bear. The exact Tripo layer is added after source validation. */
public final class LivingPolarBearRenderer extends MobRenderer<LivingPolarBearEntity, PolarBearModel<LivingPolarBearEntity>> {
    public LivingPolarBearRenderer(EntityRendererProvider.Context context) {
        super(context, new PolarBearModel<>(context.bakeLayer(ModelLayers.POLAR_BEAR)), 0.9F);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.POLAR_BEAR, CustomMobModelLayers.POLAR_BEAR_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingPolarBearEntity bear) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
