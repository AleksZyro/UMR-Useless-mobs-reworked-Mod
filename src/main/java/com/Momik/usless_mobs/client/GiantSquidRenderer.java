package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.GiantSquidEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class GiantSquidRenderer extends MobRenderer<GiantSquidEntity, SquidModel<GiantSquidEntity>> {
    public GiantSquidRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 2.7F);
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.SQUID, CustomMobModelLayers.SQUID_EXACT_TEXTURE));
    }

    @Override
    protected void scale(GiantSquidEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.23F, 2.23F, 2.23F);
    }

    @Override
    public ResourceLocation getTextureLocation(GiantSquidEntity entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
