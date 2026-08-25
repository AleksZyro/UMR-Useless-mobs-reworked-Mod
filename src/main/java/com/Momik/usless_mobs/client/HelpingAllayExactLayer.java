package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.allay.Allay;

public final class HelpingAllayExactLayer extends RenderLayer<Allay, AllayModel> {
    private static final Set<String> BONES = Set.of(
            "body",
            "head",
            "right_arm",
            "left_arm",
            "right_wing",
            "right_wing_tip",
            "left_wing",
            "left_wing_tip",
            "soul_core");

    private final ExactMobMesh mesh;

    public HelpingAllayExactLayer(RenderLayerParent<Allay, AllayModel> parent, ResourceManager resourceManager) {
        super(parent);
        try {
            this.mesh = ExactMobMesh.load(resourceManager, "helping_allay", BONES);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load exact Helping Allay mesh", exception);
        }
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Allay entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (entity.isInvisible() || !(entity instanceof HelpingAllayEntity helpingAllay)) {
            return;
        }

        VertexConsumer buffer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(CustomMobModelLayers.HELPING_ALLAY_EXACT_TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(helpingAllay, 0.0F);
        var cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        ExactAnimationLod animationLod = ExactAnimationLod.at(entity.distanceToSqr(cameraPosition));
        float lodAgeInTicks = animationLod.quantizedAge(ageInTicks);
        poseStack.pushPose();
        float modelScale = 1.35F;
        poseStack.scale(modelScale, modelScale, modelScale);
        poseStack.translate(0F, 1.5F / modelScale - 1.5F, 0F);
        if (animationLod != ExactAnimationLod.FAR) {
            poseStack.translate(0F, Mth.sin(lodAgeInTicks * 0.12F) * 0.012F, 0F);
        }
        for (String bone : BONES) {
            if (animationLod != ExactAnimationLod.FAR) {
                this.mesh.renderAllayBone(bone, poseStack, buffer,
                        LightTexture.FULL_BRIGHT, overlay, lodAgeInTicks,
                        netHeadYaw, headPitch, helpingAllay.action());
            } else {
                this.mesh.renderBone(bone, poseStack, buffer,
                        LightTexture.FULL_BRIGHT, overlay);
            }
        }
        poseStack.popPose();
    }
}
