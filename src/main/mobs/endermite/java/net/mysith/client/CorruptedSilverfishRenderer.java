package net.mysith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.mysith.silverfish.CorruptedSilverfishEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.io.IOException;

public class CorruptedSilverfishRenderer extends GeoEntityRenderer<CorruptedSilverfishEntity> {
    private static final ResourceLocation GLOWMASK = ResourceLocation.tryBuild(
            "usless_mobs", "textures/entity/corrupted_silverfish_glowmask.png");
    private final CorruptedSilverfishMesh exactMesh;

    public CorruptedSilverfishRenderer(EntityRendererProvider.Context context) {
        super(context, new CorruptedSilverfishModel());
        try {
            this.exactMesh = CorruptedSilverfishMesh.load(context.getResourceManager());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the exact Corrupted Silverfish Tripo mesh", exception);
        }
        this.shadowRadius = 0.3F;
        // The v3 glowmask is a protected candidate; register only when an active pack supplies it.
        if (context.getResourceManager().getResource(GLOWMASK).isPresent()) {
            addRenderLayer(new CorruptedSilverfishGlowLayer(this));
        }
    }

    @Override
    public void preRender(PoseStack poseStack, CorruptedSilverfishEntity animatable,
                          BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CorruptedSilverfishEntity animatable,
                                  GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);
        if (!bone.isHidden()) {
            this.exactMesh.renderBone(bone.getName(), poseStack, buffer, packedLight,
                    packedOverlay, red, green, blue, alpha);
        }
        if (!isReRender) {
            applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource,
                    buffer, partialTick, packedLight, packedOverlay);
        }
        renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    public static EntityRenderer<CorruptedSilverfishEntity> createRenderer(EntityRendererProvider.Context context) {
        return new CorruptedSilverfishRenderer(context);
    }
}
