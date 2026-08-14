package com.Momik.usless_mobs.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class CustomMob3DModel<T extends LivingEntity> extends HierarchicalModel<T> {
    public enum Variant {
        LIVING_BOSS,
        FROST_STRAY,
        WEB_CAVE_SPIDER,
        CORAL_DROWNED,
        OCTOPUS,
        WITCH_BOSS,
        LIVING_BAT,
        ROOTED_HUSK
    }

    private final ModelPart root;
    private final Variant variant;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWingTip;
    private final ModelPart[] tentacles = new ModelPart[8];

    public CustomMob3DModel(ModelPart root, Variant variant) {
        this.root = root;
        this.variant = variant;
        this.head = child("head");
        this.body = child("body");
        this.rightArm = child("right_arm");
        this.leftArm = child("left_arm");
        this.rightLeg = child("right_leg");
        this.leftLeg = child("left_leg");
        this.leftWing = child("left_wing");
        this.rightWing = child("right_wing");
        this.leftWingTip = child("left_wing_tip");
        this.rightWingTip = child("right_wing_tip");
        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i] = child("tentacle" + i);
        }
    }

    private ModelPart child(String name) {
        if (this.root.hasChild(name)) {
            return this.root.getChild(name);
        }
        return this.root.getAllParts()
                .filter(part -> part.hasChild(name))
                .findFirst()
                .map(part -> part.getChild(name))
                .orElse(null);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    public static LayerDefinition createLayer(Variant variant) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        switch (variant) {
            case LIVING_BOSS -> addLivingBoss(root);
            case FROST_STRAY -> addFrostStray(root);
            case WEB_CAVE_SPIDER -> addWebCaveSpider(root);
            case CORAL_DROWNED -> addCoralDrowned(root);
            case OCTOPUS -> addOctopus(root);
            case WITCH_BOSS -> addWitchBoss(root);
            case LIVING_BAT -> addLivingBat(root);
            case ROOTED_HUSK -> addRootedHusk(root);
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        if (this.head != null) {
            this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        }
        if (this.rightArm != null) {
            this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.25F * limbSwingAmount;
        }
        if (this.leftArm != null) {
            this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.25F * limbSwingAmount;
        }
        if (this.rightLeg != null) {
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.15F * limbSwingAmount;
        }
        if (this.leftLeg != null) {
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.15F * limbSwingAmount;
        }

        switch (this.variant) {
            case LIVING_BOSS -> animateLivingBoss(limbSwing, limbSwingAmount, ageInTicks);
            case WEB_CAVE_SPIDER -> animateSpider(limbSwing, limbSwingAmount, ageInTicks);
            case OCTOPUS -> animateOctopus(ageInTicks, entity.isInWater());
            case LIVING_BAT -> animateBat(ageInTicks);
            case WITCH_BOSS -> animateWitch(ageInTicks);
            case FROST_STRAY -> animateFrost(ageInTicks);
            case CORAL_DROWNED -> animateCoral(ageInTicks, entity.isInWater());
            case ROOTED_HUSK -> animateRooted(ageInTicks);
        }
    }

    private void animateLivingBoss(float limbSwing, float limbSwingAmount, float ageInTicks) {
        if (this.body != null) {
            this.body.y += Mth.sin(ageInTicks * 0.12F) * 0.35F;
        }
        if (this.head != null) {
            this.head.zRot += Mth.sin(ageInTicks * 0.08F) * 0.05F;
        }
        for (int i = 0; i < 4; i++) {
            ModelPart leg = child("leg_vine_" + i);
            if (leg != null) {
                leg.xRot = Mth.cos(limbSwing * 0.6662F + i * Mth.HALF_PI) * 0.45F * limbSwingAmount;
            }
        }
    }

    private void animateSpider(float limbSwing, float limbSwingAmount, float ageInTicks) {
        if (this.body != null) {
            this.body.y += Mth.sin(ageInTicks * 0.2F) * 0.25F;
        }
        for (int i = 0; i < 8; i++) {
            ModelPart leg = child("web_leg_" + i);
            if (leg != null) {
                leg.yRot += Mth.cos(limbSwing * 1.2F + i) * 0.35F * limbSwingAmount;
                leg.zRot += Mth.sin(ageInTicks * 0.11F + i) * 0.04F;
            }
        }
    }

    private void animateOctopus(float ageInTicks, boolean inWater) {
        float amount = inWater ? 1.0F : 0.35F;
        for (int i = 0; i < this.tentacles.length; i++) {
            ModelPart tentacle = this.tentacles[i];
            if (tentacle != null) {
                float phase = i * Mth.PI / 4.0F;
                tentacle.xRot += Mth.sin(ageInTicks * 0.2F + phase) * 0.35F * amount;
                tentacle.zRot += Mth.cos(ageInTicks * 0.13F + phase) * 0.22F * amount;
            }
        }
        if (this.body != null) {
            this.body.y += Mth.sin(ageInTicks * 0.08F) * 0.25F * amount;
        }
    }

    private void animateBat(float ageInTicks) {
        float flap = Mth.cos(ageInTicks * 0.75F) * 0.65F;
        if (this.rightWing != null) {
            this.rightWing.yRot = -0.85F + flap;
        }
        if (this.leftWing != null) {
            this.leftWing.yRot = 0.85F - flap;
        }
        if (this.rightWingTip != null) {
            this.rightWingTip.yRot = -0.45F + flap * 0.5F;
        }
        if (this.leftWingTip != null) {
            this.leftWingTip.yRot = 0.45F - flap * 0.5F;
        }
    }

    private void animateWitch(float ageInTicks) {
        if (this.body != null) {
            this.body.zRot += Mth.sin(ageInTicks * 0.06F) * 0.05F;
        }
        if (this.head != null) {
            this.head.yRot += Mth.sin(ageInTicks * 0.05F) * 0.08F;
        }
    }

    private void animateFrost(float ageInTicks) {
        float shiver = Mth.cos(ageInTicks * 2.7F) * 0.025F;
        if (this.body != null) {
            this.body.zRot += shiver;
        }
        if (this.head != null) {
            this.head.zRot += shiver * 1.6F;
        }
    }

    private void animateCoral(float ageInTicks, boolean inWater) {
        float current = inWater ? 1.0F : 0.25F;
        if (this.body != null) {
            this.body.zRot += Mth.sin(ageInTicks * 0.07F) * 0.08F * current;
        }
        if (this.head != null) {
            this.head.zRot += Mth.cos(ageInTicks * 0.06F) * 0.05F * current;
        }
    }

    private void animateRooted(float ageInTicks) {
        if (this.body != null) {
            this.body.zRot += Mth.sin(ageInTicks * 0.04F) * 0.035F;
        }
    }

    private static void addLivingBoss(PartDefinition root) {
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.5F, -10.5F, -8.5F, 17.0F, 18.0F, 22.0F, new CubeDeformation(0.18F))
                        .texOffs(0, 40).addBox(-6.5F, -14.0F, -7.5F, 13.0F, 4.0F, 18.0F, new CubeDeformation(0.12F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, Mth.HALF_PI, 0.0F, 0.0F));
        body.addOrReplaceChild("back_root_left", CubeListBuilder.create().texOffs(42, 40)
                        .addBox(-9.6F, -12.0F, -2.0F, 3.0F, 20.0F, 3.0F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.28F, 0.0F, -0.18F));
        body.addOrReplaceChild("back_root_right", CubeListBuilder.create().texOffs(54, 40)
                        .addBox(6.6F, -12.0F, -2.0F, 3.0F, 20.0F, 3.0F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.28F, 0.0F, 0.18F));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-8.8F, -20.5F, -15.2F, 17.6F, 8.0F, 7.0F, new CubeDeformation(0.16F))
                        .texOffs(36, 20).addBox(-3.0F, -23.8F, -16.2F, 6.0F, 5.0F, 3.0F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        head.addOrReplaceChild("left_branch_horn", CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-12.8F, -23.0F, -11.4F, 4.0F, 3.0F, 10.0F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.0F, -0.1F, -0.42F));
        head.addOrReplaceChild("right_branch_horn", CubeListBuilder.create().texOffs(48, 0).mirror()
                        .addBox(8.8F, -23.0F, -11.4F, 4.0F, 3.0F, 10.0F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.0F, 0.1F, 0.42F));

        addRavagerVineLeg(root, "leg_vine_0", -8.0F, -10.0F, -6.0F);
        addRavagerVineLeg(root, "leg_vine_1", 8.0F, -10.0F, -6.0F);
        addRavagerVineLeg(root, "leg_vine_2", -8.0F, -10.0F, 16.0F);
        addRavagerVineLeg(root, "leg_vine_3", 8.0F, -10.0F, 16.0F);
    }

    private static void addRavagerVineLeg(PartDefinition root, String name, float x, float y, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(28, 40)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 29.0F, 3.0F, new CubeDeformation(0.04F)),
                PartPose.offset(x, y, z));
    }

    private static void addFrostStray(PartDefinition root) {
        PartDefinition head = addHumanoidHead(root);
        head.addOrReplaceChild("ice_crown", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.8F, -9.6F, -4.8F, 9.6F, 2.0F, 9.6F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        head.addOrReplaceChild("front_icicle", CubeListBuilder.create().texOffs(0, 12)
                        .addBox(-1.0F, -13.0F, -5.2F, 2.0F, 4.2F, 2.0F, new CubeDeformation(0.04F)),
                PartPose.rotation(-0.18F, 0.0F, 0.0F));
        addHumanoidBody(root).addOrReplaceChild("frost_spine", CubeListBuilder.create().texOffs(10, 12)
                        .addBox(-1.4F, 0.0F, 2.4F, 2.8F, 13.0F, 2.0F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.14F, 0.0F, 0.0F));
        addHumanoidLimbs(root, 0.18F);
    }

    private static void addWebCaveSpider(PartDefinition root) {
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.5F, -4.2F, -8.0F, 9.0F, 6.0F, 7.0F, new CubeDeformation(0.08F))
                        .texOffs(28, 0).addBox(-3.6F, -5.0F, -8.8F, 7.2F, 1.5F, 2.0F, new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, 15.0F, -3.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-5.6F, -4.8F, -3.6F, 11.2F, 8.8F, 10.8F, new CubeDeformation(0.10F))
                        .texOffs(36, 16).addBox(-4.0F, -6.4F, 0.0F, 8.0F, 2.6F, 7.0F, new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, 15.0F, 4.0F));
        body.addOrReplaceChild("web_saddle", CubeListBuilder.create().texOffs(0, 36)
                        .addBox(-5.2F, -7.4F, -2.0F, 10.4F, 2.0F, 10.0F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.0F, 0.0F, 0.0F));
        for (int i = 0; i < 8; i++) {
            float side = i < 4 ? -1.0F : 1.0F;
            float z = -4.5F + (i % 4) * 3.1F;
            root.addOrReplaceChild("web_leg_" + i, CubeListBuilder.create().texOffs(44, 28)
                            .addBox(side < 0 ? -14.0F : 0.0F, -0.8F, -0.8F, 14.0F, 1.6F, 1.6F, new CubeDeformation(0.04F)),
                    PartPose.offsetAndRotation(side * 4.8F, 15.2F, z, 0.0F, side * (0.42F + (i % 2) * 0.18F), side * 0.28F));
        }
    }

    private static void addCoralDrowned(PartDefinition root) {
        PartDefinition head = addHumanoidHead(root);
        head.addOrReplaceChild("coral_head_plate", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.8F, -8.9F, -4.9F, 9.6F, 2.3F, 9.8F, new CubeDeformation(0.10F)),
                PartPose.ZERO);
        head.addOrReplaceChild("left_coral_antler", CubeListBuilder.create().texOffs(0, 12)
                        .addBox(-7.2F, -12.0F, -2.0F, 2.2F, 5.5F, 2.2F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.0F, 0.0F, -0.34F));
        head.addOrReplaceChild("right_coral_antler", CubeListBuilder.create().texOffs(8, 12)
                        .addBox(5.0F, -12.0F, -2.0F, 2.2F, 5.5F, 2.2F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.0F, 0.0F, 0.34F));
        PartDefinition body = addHumanoidBody(root);
        body.addOrReplaceChild("reef_shell", CubeListBuilder.create().texOffs(16, 12)
                        .addBox(-5.0F, 1.0F, 1.7F, 10.0F, 11.0F, 2.8F, new CubeDeformation(0.12F)),
                PartPose.rotation(0.08F, 0.0F, 0.0F));
        addHumanoidLimbs(root, 0.20F);
    }

    private static void addOctopus(PartDefinition root) {
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, -9.0F, -7.0F, 14.0F, 16.0F, 14.0F, new CubeDeformation(0.12F))
                        .texOffs(0, 30).addBox(-5.2F, -12.4F, -5.2F, 10.4F, 5.0F, 10.4F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        body.addOrReplaceChild("mantle_fin_left", CubeListBuilder.create().texOffs(36, 34)
                        .addBox(-9.4F, -6.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.03F)),
                PartPose.rotation(0.0F, 0.0F, -0.25F));
        body.addOrReplaceChild("mantle_fin_right", CubeListBuilder.create().texOffs(48, 34)
                        .addBox(6.4F, -6.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.03F)),
                PartPose.rotation(0.0F, 0.0F, 0.25F));

        CubeListBuilder tentacle = CubeListBuilder.create().texOffs(44, 0)
                .addBox(-1.2F, 0.0F, -1.2F, 2.4F, 20.0F, 2.4F, new CubeDeformation(0.04F))
                .texOffs(54, 0).addBox(-1.8F, 12.0F, -1.8F, 3.6F, 6.0F, 3.6F, new CubeDeformation(0.02F));
        for (int i = 0; i < 8; i++) {
            float angle = i * Mth.TWO_PI / 8.0F;
            float x = Mth.cos(angle) * 5.3F;
            float z = Mth.sin(angle) * 5.3F;
            root.addOrReplaceChild("tentacle" + i, tentacle,
                    PartPose.offsetAndRotation(x, 15.0F, z, 0.0F, -angle + Mth.HALF_PI, 0.0F));
        }
    }

    private static void addWitchBoss(PartDefinition root) {
        PartDefinition head = addHumanoidHead(root);
        head.addOrReplaceChild("witch_crown_ring", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, -9.0F, -5.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.10F)),
                PartPose.ZERO);
        head.addOrReplaceChild("witch_crystal_hat", CubeListBuilder.create().texOffs(0, 14)
                        .addBox(-2.5F, -16.0F, -2.5F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.08F)),
                PartPose.rotation(0.12F, 0.0F, -0.08F));
        PartDefinition body = addHumanoidBody(root);
        body.addOrReplaceChild("spell_collar", CubeListBuilder.create().texOffs(24, 14)
                        .addBox(-6.0F, -1.2F, -3.5F, 12.0F, 3.0F, 7.0F, new CubeDeformation(0.12F)),
                PartPose.ZERO);
        addHumanoidLimbs(root, 0.16F);
    }

    private static void addLivingBat(PartDefinition root) {
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -3.5F, -3.6F, 7.0F, 7.0F, 7.2F, new CubeDeformation(0.05F))
                        .texOffs(28, 0).addBox(-1.0F, -6.2F, -3.8F, 2.0F, 3.2F, 1.8F, new CubeDeformation(0.03F)),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.8F, 4.0F, -3.4F, 7.6F, 12.5F, 6.8F, new CubeDeformation(0.06F))
                        .texOffs(28, 18).addBox(-2.0F, 15.5F, -0.4F, 4.0F, 5.0F, 1.2F, new CubeDeformation(0.03F)),
                PartPose.ZERO);
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(38, 0)
                        .addBox(-13.0F, 1.0F, 1.3F, 11.0F, 17.0F, 1.2F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
        rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().texOffs(38, 20)
                        .addBox(-8.4F, 1.0F, -0.1F, 8.4F, 12.8F, 1.1F, new CubeDeformation(0.02F)),
                PartPose.offset(-13.0F, 1.0F, 1.3F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(38, 0).mirror()
                        .addBox(2.0F, 1.0F, 1.3F, 11.0F, 17.0F, 1.2F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
        leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().texOffs(38, 20).mirror()
                        .addBox(0.0F, 1.0F, -0.1F, 8.4F, 12.8F, 1.1F, new CubeDeformation(0.02F)),
                PartPose.offset(13.0F, 1.0F, 1.3F));
    }

    private static void addRootedHusk(PartDefinition root) {
        PartDefinition head = addHumanoidHead(root);
        head.addOrReplaceChild("root_face_mask", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.7F, -8.7F, -5.0F, 9.4F, 9.0F, 2.0F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        PartDefinition body = addHumanoidBody(root);
        body.addOrReplaceChild("root_rib_wrap", CubeListBuilder.create().texOffs(0, 14)
                        .addBox(-5.4F, 1.2F, -3.1F, 10.8F, 10.6F, 6.2F, new CubeDeformation(0.14F)),
                PartPose.rotation(0.0F, 0.0F, 0.06F));
        body.addOrReplaceChild("back_vine", CubeListBuilder.create().texOffs(34, 14)
                        .addBox(-1.2F, -1.2F, 2.7F, 2.4F, 14.6F, 2.0F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.14F, 0.0F, 0.0F));
        addHumanoidLimbs(root, 0.22F);
    }

    private static PartDefinition addHumanoidHead(PartDefinition root) {
        return root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-4.25F, -8.25F, -4.25F, 8.5F, 8.5F, 8.5F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
    }

    private static PartDefinition addHumanoidBody(PartDefinition root) {
        return root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(26, 48).addBox(-4.4F, -0.2F, -2.4F, 8.8F, 12.4F, 4.8F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
    }

    private static void addHumanoidLimbs(PartDefinition root, float inflate) {
        CubeDeformation deformation = new CubeDeformation(inflate);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 48)
                        .addBox(-3.2F, -2.0F, -2.3F, 4.2F, 12.2F, 4.6F, deformation),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 48).mirror()
                        .addBox(-1.0F, -2.0F, -2.3F, 4.2F, 12.2F, 4.6F, deformation),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(52, 32)
                        .addBox(-2.2F, 0.0F, -2.2F, 4.4F, 12.3F, 4.4F, deformation),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(52, 32).mirror()
                        .addBox(-2.2F, 0.0F, -2.2F, 4.4F, 12.3F, 4.4F, deformation),
                PartPose.offset(2.0F, 12.0F, 0.0F));
    }
}
