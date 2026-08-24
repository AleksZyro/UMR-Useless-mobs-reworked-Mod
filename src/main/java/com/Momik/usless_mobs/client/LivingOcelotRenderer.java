package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.LivingOcelotEntity;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders only the dedicated UMR ocelot with its exact 4K Tripo surface. */
public final class LivingOcelotRenderer extends MobRenderer<LivingOcelotEntity, OcelotModel<LivingOcelotEntity>> {
    public LivingOcelotRenderer(EntityRendererProvider.Context context) {
        super(context, new OcelotModel<>(context.bakeLayer(ModelLayers.OCELOT)), 0.50F);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.OCELOT, CustomMobModelLayers.OCELOT_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingOcelotEntity ocelot) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
