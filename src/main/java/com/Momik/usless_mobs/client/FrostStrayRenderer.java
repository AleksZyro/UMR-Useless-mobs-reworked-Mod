package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public class FrostStrayRenderer extends StrayRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/living/stray.png");

    public FrostStrayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FrostStrayModel<>(context.bakeLayer(ModelLayers.STRAY));
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.FROST_STRAY),
                CustomMob3DModel.Variant.FROST_STRAY, CustomMobModelLayers.FROST_STRAY_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
        if (entity instanceof FrostStrayEntity) {
            return TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
