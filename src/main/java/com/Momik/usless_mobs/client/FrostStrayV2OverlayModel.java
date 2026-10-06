package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Twelve detachable, seam-safe ice accents used by the v2 visual preview. */
final class FrostStrayV2OverlayModel {
    private final ModelPart root;

    FrostStrayV2OverlayModel(ModelPart root) {
        this.root = root;
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation edge = new CubeDeformation(0.08F);
        root.addOrReplaceChild("ice_crown", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.8F, -10.2F, -4.8F, 9.6F, 1.2F, 9.6F, edge),
                PartPose.ZERO);
        root.addOrReplaceChild("ice_crown_left", CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-5.8F, -13.0F, -1.0F, 2.0F, 3.4F, 2.0F, edge),
                PartPose.rotation(0.0F, 0.0F, -0.18F));
        root.addOrReplaceChild("ice_crown_right", CubeListBuilder.create().texOffs(24, 0)
                        .addBox(3.8F, -13.0F, -1.0F, 2.0F, 3.4F, 2.0F, edge),
                PartPose.rotation(0.0F, 0.0F, 0.18F));
        root.addOrReplaceChild("ice_face_shard", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.0F, -6.5F, -5.1F, 2.0F, 3.2F, 1.4F, edge),
                PartPose.rotation(-0.20F, 0.0F, 0.0F));
        root.addOrReplaceChild("ice_collar", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-5.0F, -0.4F, -3.0F, 10.0F, 1.5F, 6.0F, edge),
                PartPose.ZERO);
        root.addOrReplaceChild("ice_shoulder_left", CubeListBuilder.create().texOffs(20, 16)
                        .addBox(3.8F, 0.0F, -2.8F, 3.2F, 2.0F, 5.6F, edge),
                PartPose.rotation(0.0F, 0.0F, -0.16F));
        root.addOrReplaceChild("ice_shoulder_right", CubeListBuilder.create().texOffs(20, 24)
                        .addBox(-7.0F, 0.0F, -2.8F, 3.2F, 2.0F, 5.6F, edge),
                PartPose.rotation(0.0F, 0.0F, 0.16F));
        root.addOrReplaceChild("ice_forearm_left", CubeListBuilder.create().texOffs(36, 16)
                        .addBox(4.8F, 5.0F, -2.4F, 2.2F, 5.6F, 4.8F, edge),
                PartPose.rotation(0.0F, 0.0F, -0.08F));
        root.addOrReplaceChild("ice_forearm_right", CubeListBuilder.create().texOffs(36, 24)
                        .addBox(-7.0F, 5.0F, -2.4F, 2.2F, 5.6F, 4.8F, edge),
                PartPose.rotation(0.0F, 0.0F, 0.08F));
        root.addOrReplaceChild("ice_spine_upper", CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.4F, 1.0F, 2.2F, 2.8F, 5.2F, 2.0F, edge),
                PartPose.rotation(0.12F, 0.0F, 0.0F));
        root.addOrReplaceChild("ice_spine_lower", CubeListBuilder.create().texOffs(12, 32)
                        .addBox(-1.2F, 6.0F, 2.3F, 2.4F, 5.2F, 1.8F, edge),
                PartPose.rotation(0.14F, 0.0F, 0.0F));
        root.addOrReplaceChild("ice_hip_shards", CubeListBuilder.create().texOffs(24, 32)
                        .addBox(-4.8F, 10.2F, -2.5F, 9.6F, 1.8F, 5.0F, edge),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    void setupAnim(float ageInTicks, float abilityProgress) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float pulse = 0.04F + Mth.sin(ageInTicks * 0.22F) * 0.025F;
        float charge = Mth.sin(abilityProgress * Mth.PI) * 0.22F;
        this.root.getChild("ice_crown_left").zRot -= pulse + charge;
        this.root.getChild("ice_crown_right").zRot += pulse + charge;
        this.root.getChild("ice_face_shard").xRot -= charge * 0.7F;
        this.root.getChild("ice_shoulder_left").zRot -= pulse * 0.7F + charge;
        this.root.getChild("ice_shoulder_right").zRot += pulse * 0.7F + charge;
        this.root.getChild("ice_spine_upper").xRot += charge * 0.5F;
        this.root.getChild("ice_spine_lower").xRot += charge * 0.35F;
        this.root.getChild("ice_hip_shards").yRot += Mth.sin(ageInTicks * 0.12F) * 0.03F;
    }

    void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
