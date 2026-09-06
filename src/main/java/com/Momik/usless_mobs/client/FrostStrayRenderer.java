package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.FrostStrayEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public class FrostStrayRenderer extends StrayRenderer {
    public FrostStrayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FrostStrayModel<>(context.bakeLayer(ModelLayers.STRAY));
        this.layers.clear();
        this.addLayer(new ExactHeldItemLayer<>(this, context.getItemInHandRenderer(),
                CustomMob3DModel.Variant.FROST_STRAY));
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.FROST_STRAY, CustomMobModelLayers.FROST_STRAY_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
        if (entity instanceof FrostStrayEntity) {
            return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
