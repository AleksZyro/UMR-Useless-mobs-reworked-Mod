package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.FrostStrayV2Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Renders the v2 ice accents without splitting the connected source mesh. */
final class FrostStrayV2OverlayLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE = CustomMobModelLayers.FROST_STRAY_V2_OVERLAY_TEXTURE;
    private final FrostStrayV2OverlayModel model;

    FrostStrayV2OverlayLayer(RenderLayerParent<T, M> parent, ModelPart root) {
        super(parent);
        this.model = new FrostStrayV2OverlayModel(root);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof FrostStrayV2Entity frostStray) || entity.isInvisible()) {
            return;
        }
        float progress = FrostStrayAbilityClient.progressFor(frostStray, partialTicks);
        this.model.setupAnim(ageInTicks, progress);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        poseStack.pushPose();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (entity.distanceToSqr(camera) > 24.0D * 24.0D) {
            // Preserve the silhouette cue at distance while avoiding animated detail work.
            this.model.setupAnim(0.0F, progress);
        }
        this.model.render(poseStack, buffer, LightTexture.FULL_BRIGHT, overlay);
        poseStack.popPose();
    }
}
