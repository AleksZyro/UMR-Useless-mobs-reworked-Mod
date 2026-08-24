package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CelestialSlimeRenderer extends GeoEntityRenderer<CelestialSlimeEntity> {

    public CelestialSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new CelestialSlimeModel());
        this.shadowRadius = 0.7F;
    }

    public static EntityRenderer<CelestialSlimeEntity> createRenderer(EntityRendererProvider.Context context) {
        return new CelestialSlimeRenderer(context);
    }

    @Override
    public void preRender(PoseStack poseStack, CelestialSlimeEntity entity, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        float scale = entity.getSize() * 0.5F;
        this.shadowRadius = Math.max(0.45F, entity.getSize() * 0.35F);
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
