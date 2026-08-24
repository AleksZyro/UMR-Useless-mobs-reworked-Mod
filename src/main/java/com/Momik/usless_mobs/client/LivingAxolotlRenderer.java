package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.LivingAxolotlEntity;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders only the dedicated UMR axolotl with its exact 4K Tripo surface. */
public final class LivingAxolotlRenderer extends MobRenderer<LivingAxolotlEntity, AxolotlModel<LivingAxolotlEntity>> {
    public LivingAxolotlRenderer(EntityRendererProvider.Context context) {
        super(context, new AxolotlModel<>(context.bakeLayer(ModelLayers.AXOLOTL)), 0.55F);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.AXOLOTL, CustomMobModelLayers.AXOLOTL_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingAxolotlEntity axolotl) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
