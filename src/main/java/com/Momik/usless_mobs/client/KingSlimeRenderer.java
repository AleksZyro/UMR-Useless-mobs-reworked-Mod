package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KingSlimeRenderer extends GeoEntityRenderer<KingSlimeEntity> {

    public KingSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new KingSlimeModel());
        this.shadowRadius = 1.8F;
    }

    public static EntityRenderer<KingSlimeEntity> createRenderer(EntityRendererProvider.Context context) {
        return new KingSlimeRenderer(context);
    }

    @Override
    public void preRender(PoseStack poseStack, KingSlimeEntity entity, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        // Vanilla Slime hitbox = base 2.04 * 0.255 * size = ~4.16 blocks at size 8.
        // Our bbmodel cube is 16 model-units = 1 block at unit scale, so scale up by ~4.
        // entity.getSize() ranges 5-8 -> 2.5 to 4.0 scale matches the hitbox.
        float scale = entity.getSize() * 0.5F;
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
