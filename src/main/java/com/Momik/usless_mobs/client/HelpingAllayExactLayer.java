package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.Set;
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
import org.joml.Vector3f;

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
        poseStack.pushPose();
        float modelScale = 1.35F;
        poseStack.scale(modelScale, modelScale, modelScale);
        poseStack.translate(0F, 1.5F / modelScale - 1.5F, 0F);
        poseStack.translate(0F, Mth.sin(ageInTicks * 0.12F) * 0.012F, 0F);
        for (String bone : BONES) {
            // The source has shared vertices but no skin weights. Independent
            // bone rotations would split the Allay's continuous surface.
            BonePose animation = BonePose.ZERO;
            Vector3f pivot = this.mesh.pivot(bone);
            poseStack.pushPose();
            poseStack.translate(
                    (pivot.x() + animation.x()) / 16F,
                    (pivot.y() + animation.y()) / 16F,
                    (pivot.z() + animation.z()) / 16F);
            if (animation.zRot() != 0F) {
                poseStack.mulPose(Axis.ZP.rotation(animation.zRot()));
            }
            if (animation.yRot() != 0F) {
                poseStack.mulPose(Axis.YP.rotation(animation.yRot()));
            }
            if (animation.xRot() != 0F) {
                poseStack.mulPose(Axis.XP.rotation(animation.xRot()));
            }
            poseStack.translate(-pivot.x() / 16F, -pivot.y() / 16F, -pivot.z() / 16F);
            this.mesh.renderBone(
                    bone,
                    poseStack,
                    buffer,
                    LightTexture.FULL_BRIGHT,
                    overlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static BonePose poseFor(
            String bone,
            HelpingAllayEntity entity,
            float age,
            float netHeadYaw,
            float headPitch) {
        byte action = entity.action();
        if (bone.equals("head")) {
            return new BonePose(
                    0F, 0F, 0F,
                    headPitch * Mth.DEG_TO_RAD,
                    netHeadYaw * Mth.DEG_TO_RAD,
                    0F);
        }
        if (bone.equals("body")) {
            float hover = Mth.sin(age * 0.16F) * 0.22F;
            float roll = action == HelpingAllayEntity.ACTION_TELEPORT
                    ? Mth.sin(age * 0.75F) * 0.16F
                    : Mth.sin(age * 0.05F) * 0.025F;
            return new BonePose(0F, hover, 0F, 0F, 0F, roll);
        }
        if (bone.equals("soul_core")) {
            float pulse = action == HelpingAllayEntity.ACTION_HEAL ? 0.36F : 0.10F;
            return new BonePose(0F, Mth.sin(age * 0.42F) * pulse, pulse, 0F, 0F, 0F);
        }
        if (bone.equals("right_arm") || bone.equals("left_arm")) {
            boolean left = bone.equals("left_arm");
            float side = left ? -1F : 1F;
            if (action == HelpingAllayEntity.ACTION_REVEAL) {
                return new BonePose(0F, 0F, 0F, -1.05F, side * 0.16F, side * 0.18F);
            }
            if (action == HelpingAllayEntity.ACTION_SHIELD) {
                return new BonePose(0F, 0F, 0F, -1.24F, side * 0.34F, side * 0.34F);
            }
            if (action == HelpingAllayEntity.ACTION_HEAL || action == HelpingAllayEntity.ACTION_BOND) {
                return new BonePose(0F, 0F, 0F, -0.72F, side * 0.28F, side * 0.12F);
            }
            return new BonePose(0F, 0F, 0F, Mth.sin(age * 0.10F) * 0.08F, 0F, side * 0.14F);
        }
        if (bone.contains("wing")) {
            boolean left = bone.startsWith("left");
            boolean tip = bone.endsWith("tip");
            float side = left ? -1F : 1F;
            if (action == HelpingAllayEntity.ACTION_TELEPORT) {
                return new BonePose(0F, 0F, 0F, 0F, side * 1.18F, side * 0.12F);
            }
            float speed = action == HelpingAllayEntity.ACTION_SHIELD
                    || action == HelpingAllayEntity.ACTION_HEAL ? 1.28F : 0.82F;
            float strength = action == HelpingAllayEntity.ACTION_SHIELD
                    || action == HelpingAllayEntity.ACTION_HEAL ? 0.98F : 0.62F;
            float flap = Mth.cos(age * speed) * strength * (tip ? 0.72F : 1F);
            return new BonePose(0F, 0F, 0F, 0F, side * flap * 0.18F, side * (0.14F + flap));
        }
        return BonePose.ZERO;
    }

    private record BonePose(float x, float y, float z, float xRot, float yRot, float zRot) {
        private static final BonePose ZERO = new BonePose(0F, 0F, 0F, 0F, 0F, 0F);
    }
}
