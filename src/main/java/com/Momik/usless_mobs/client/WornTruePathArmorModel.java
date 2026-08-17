package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.item.TruePathArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;

public class WornTruePathArmorModel extends HumanoidModel<LivingEntity> {
    private WornTruePathArmorModel(ModelPart root) {
        super(root);
    }

    public static WornTruePathArmorModel create(TruePathArmorItem.Path path, ArmorItem.Type type) {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.45F), 0.0F);
        PartDefinition root = mesh.getRoot();

        addBaseVolume(root, type);
        if (type == ArmorItem.Type.HELMET) {
            addPathCrown(root, path);
        }
        if (type == ArmorItem.Type.CHESTPLATE) {
            if (path == TruePathArmorItem.Path.VOID) {
                addVoidCrystalKnightDetails(root);
            } else {
                addChestDetails(root, path);
            }
        }
        if (type == ArmorItem.Type.LEGGINGS) {
            addLegDetails(root, path);
        }
        if (type == ArmorItem.Type.BOOTS) {
            addBootDetails(root, path);
        }

        return new WornTruePathArmorModel(LayerDefinition.create(mesh, 128, 64).bakeRoot());
    }

    public static WornTruePathArmorModel createBalancedCrown() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.42F), 0.0F);
        PartDefinition root = mesh.getRoot();
        addBalancedCrown(root);
        return new WornTruePathArmorModel(LayerDefinition.create(mesh, 128, 64).bakeRoot());
    }

    public static WornTruePathArmorModel createBalanced(ArmorItem.Type type) {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.45F), 0.0F);
        PartDefinition root = mesh.getRoot();

        addBaseVolume(root, type);
        if (type == ArmorItem.Type.HELMET) {
            addBalancedCrown(root);
        }
        if (type == ArmorItem.Type.CHESTPLATE) {
            addLegacyBalanceVoidDetails(root);
            addChestDetails(root, TruePathArmorItem.Path.LIVING);
            root.getChild("body").addOrReplaceChild("balance_celestial_core",
                    CubeListBuilder.create().texOffs(94, 48)
                            .addBox(-1.2F, 1.0F, -3.45F, 2.4F, 2.4F, 1.1F, new CubeDeformation(0.08F)),
                    PartPose.rotation(0.0F, 0.0F, 0.78F));
        }
        if (type == ArmorItem.Type.LEGGINGS) {
            addLegDetails(root, TruePathArmorItem.Path.CELESTIAL);
            root.getChild("right_leg").addOrReplaceChild("balance_right_void_band",
                    CubeListBuilder.create().texOffs(106, 30)
                            .addBox(-2.6F, 7.0F, -3.15F, 5.2F, 1.0F, 1.0F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, -0.16F));
            root.getChild("left_leg").addOrReplaceChild("balance_left_living_band",
                    CubeListBuilder.create().texOffs(106, 34)
                            .addBox(-2.6F, 7.0F, -3.15F, 5.2F, 1.0F, 1.0F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, 0.16F));
        }
        if (type == ArmorItem.Type.BOOTS) {
            addBootDetails(root, TruePathArmorItem.Path.LIVING);
            root.getChild("right_leg").addOrReplaceChild("balance_right_toe_gem",
                    CubeListBuilder.create().texOffs(104, 44)
                            .addBox(-0.8F, 10.0F, -4.55F, 1.6F, 1.2F, 1.0F, new CubeDeformation(0.04F)),
                    PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("balance_left_toe_gem",
                    CubeListBuilder.create().texOffs(112, 44)
                            .addBox(-0.8F, 10.0F, -4.55F, 1.6F, 1.2F, 1.0F, new CubeDeformation(0.04F)),
                    PartPose.ZERO);
        }

        return new WornTruePathArmorModel(LayerDefinition.create(mesh, 128, 64).bakeRoot());
    }

    private static void addBalancedCrown(PartDefinition root) {
        addPathCrown(root, TruePathArmorItem.Path.CELESTIAL);
        root.getChild("head").addOrReplaceChild("balance_front_gem",
                CubeListBuilder.create().texOffs(88, 0)
                        .addBox(-1.0F, -10.7F, -4.95F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.12F)),
                PartPose.ZERO);
        root.getChild("head").addOrReplaceChild("balance_void_side_horn",
                CubeListBuilder.create().texOffs(104, 0)
                        .addBox(-6.4F, -10.6F, -1.0F, 2.8F, 1.4F, 2.0F, new CubeDeformation(0.06F)),
                PartPose.rotation(0.0F, 0.0F, -0.48F));
        root.getChild("head").addOrReplaceChild("balance_living_vine",
                CubeListBuilder.create().texOffs(114, 0)
                        .addBox(3.4F, -10.8F, -4.95F, 2.8F, 1.0F, 1.0F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.0F, 0.0F, 0.36F));
    }

    public static void showForType(HumanoidModel<?> model, ArmorItem.Type type) {
        model.setAllVisible(false);
        switch (type) {
            case HELMET -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHESTPLATE -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGGINGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case BOOTS -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
        }
    }

    public static void showCrown(HumanoidModel<?> model) {
        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;
    }

    private static void addBaseVolume(PartDefinition root, ArmorItem.Type type) {
        if (type == ArmorItem.Type.HELMET) {
            root.getChild("head").addOrReplaceChild("true_outer_helm",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-4.5F, -8.5F, -4.5F, 9.0F, 8.5F, 9.0F, new CubeDeformation(0.28F)),
                    PartPose.ZERO);
        }
        if (type == ArmorItem.Type.CHESTPLATE) {
            root.getChild("body").addOrReplaceChild("true_breastplate",
                    CubeListBuilder.create().texOffs(0, 18)
                            .addBox(-4.7F, -0.2F, -2.8F, 9.4F, 12.4F, 5.6F, new CubeDeformation(0.24F)),
                    PartPose.ZERO);
            root.getChild("right_arm").addOrReplaceChild("true_right_arm_plate",
                    CubeListBuilder.create().texOffs(34, 18)
                            .addBox(-3.6F, -2.2F, -2.7F, 4.2F, 6.0F, 5.4F, new CubeDeformation(0.18F)),
                    PartPose.ZERO);
            root.getChild("left_arm").addOrReplaceChild("true_left_arm_plate",
                    CubeListBuilder.create().texOffs(52, 18)
                            .addBox(-0.6F, -2.2F, -2.7F, 4.2F, 6.0F, 5.4F, new CubeDeformation(0.18F)),
                    PartPose.ZERO);
        }
        if (type == ArmorItem.Type.LEGGINGS) {
            root.getChild("body").addOrReplaceChild("true_belt",
                    CubeListBuilder.create().texOffs(0, 38)
                            .addBox(-4.6F, 9.0F, -2.7F, 9.2F, 3.2F, 5.4F, new CubeDeformation(0.22F)),
                    PartPose.ZERO);
            root.getChild("right_leg").addOrReplaceChild("true_right_thigh",
                    CubeListBuilder.create().texOffs(30, 38)
                            .addBox(-2.5F, -0.1F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.18F)),
                    PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("true_left_thigh",
                    CubeListBuilder.create().texOffs(50, 38)
                            .addBox(-2.5F, -0.1F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.18F)),
                    PartPose.ZERO);
        }
        if (type == ArmorItem.Type.BOOTS) {
            root.getChild("right_leg").addOrReplaceChild("true_right_boot",
                    CubeListBuilder.create().texOffs(70, 36)
                            .addBox(-2.6F, 6.1F, -2.9F, 5.2F, 6.2F, 5.8F, new CubeDeformation(0.22F)),
                    PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("true_left_boot",
                    CubeListBuilder.create().texOffs(92, 36)
                            .addBox(-2.6F, 6.1F, -2.9F, 5.2F, 6.2F, 5.8F, new CubeDeformation(0.22F)),
                    PartPose.ZERO);
        }
    }

    private static void addPathCrown(PartDefinition root, TruePathArmorItem.Path path) {
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("true_crown_band",
                CubeListBuilder.create().texOffs(72, 0)
                        .addBox(-5.0F, -9.0F, -5.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.18F)),
                PartPose.ZERO);
        head.addOrReplaceChild("true_crown_center_spike",
                CubeListBuilder.create().texOffs(0, 52)
                        .addBox(-1.0F, -12.2F, -5.15F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.10F)),
                PartPose.ZERO);
        head.addOrReplaceChild("true_crown_left_spike",
                CubeListBuilder.create().texOffs(8, 52)
                        .addBox(-4.2F, -11.0F, -5.0F, 1.8F, 3.2F, 1.8F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        head.addOrReplaceChild("true_crown_right_spike",
                CubeListBuilder.create().texOffs(16, 52)
                        .addBox(2.4F, -11.0F, -5.0F, 1.8F, 3.2F, 1.8F, new CubeDeformation(0.08F)),
                PartPose.ZERO);

        if (path == TruePathArmorItem.Path.VOID) {
            head.addOrReplaceChild("true_void_left_horn",
                    CubeListBuilder.create().texOffs(24, 52)
                            .addBox(-6.2F, -10.4F, -1.2F, 2.5F, 1.5F, 2.4F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, -0.42F));
            head.addOrReplaceChild("true_void_right_horn",
                    CubeListBuilder.create().texOffs(34, 52)
                            .addBox(3.7F, -10.4F, -1.2F, 2.5F, 1.5F, 2.4F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, 0.42F));
        } else if (path == TruePathArmorItem.Path.CELESTIAL) {
            head.addOrReplaceChild("true_celestial_left_wing",
                    CubeListBuilder.create().texOffs(44, 52)
                            .addBox(-6.4F, -8.6F, -1.0F, 2.6F, 3.2F, 1.8F, new CubeDeformation(0.05F)),
                    PartPose.rotation(0.0F, 0.0F, -0.28F));
            head.addOrReplaceChild("true_celestial_right_wing",
                    CubeListBuilder.create().texOffs(54, 52)
                            .addBox(3.8F, -8.6F, -1.0F, 2.6F, 3.2F, 1.8F, new CubeDeformation(0.05F)),
                    PartPose.rotation(0.0F, 0.0F, 0.28F));
        } else {
            head.addOrReplaceChild("true_living_vine",
                    CubeListBuilder.create().texOffs(64, 52)
                            .addBox(-4.7F, -9.8F, -5.35F, 9.4F, 1.0F, 1.0F, new CubeDeformation(0.08F)),
                    PartPose.rotation(0.0F, 0.0F, 0.12F));
            head.addOrReplaceChild("true_living_leaf",
                    CubeListBuilder.create().texOffs(86, 52)
                            .addBox(1.0F, -11.2F, -5.3F, 2.0F, 1.2F, 1.2F, new CubeDeformation(0.05F)),
                    PartPose.rotation(0.0F, 0.0F, 0.42F));
        }
    }

    private static void addLegacyBalanceVoidDetails(PartDefinition root) {
        root.getChild("body").addOrReplaceChild("balance_void_rib",
                    CubeListBuilder.create().texOffs(108, 52)
                            .addBox(-4.0F, 5.6F, -3.12F, 8.0F, 1.0F, 0.8F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, -0.20F));
        root.getChild("body").addOrReplaceChild("balance_void_chest_left",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-5.05F, 0.15F, -3.35F, 4.55F, 5.05F, 1.05F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, -0.04F));
        root.getChild("body").addOrReplaceChild("balance_void_chest_right",
                    CubeListBuilder.create().texOffs(14, 0)
                            .addBox(0.50F, 0.15F, -3.35F, 4.55F, 5.05F, 1.05F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, 0.04F));
        root.getChild("body").addOrReplaceChild("balance_void_chest_keel",
                    CubeListBuilder.create().texOffs(28, 0)
                            .addBox(-0.70F, 1.40F, -3.60F, 1.40F, 6.80F, 1.30F, new CubeDeformation(0.05F)),
                    PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("balance_void_abdomen_upper",
                    CubeListBuilder.create().texOffs(36, 0)
                            .addBox(-4.20F, 5.10F, -3.32F, 8.40F, 2.10F, 1.05F, new CubeDeformation(0.05F)),
                    PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("balance_void_abdomen_lower",
                    CubeListBuilder.create().texOffs(58, 0)
                            .addBox(-3.60F, 7.20F, -3.28F, 7.20F, 2.00F, 1.00F, new CubeDeformation(0.05F)),
                    PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("balance_void_back_shell",
                    CubeListBuilder.create().texOffs(78, 0)
                            .addBox(-4.40F, 0.40F, 2.28F, 8.80F, 8.80F, 1.00F, new CubeDeformation(0.05F)),
                    PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("balance_void_right_shoulder_cap",
                    CubeListBuilder.create().texOffs(100, 0)
                            .addBox(-4.15F, -3.40F, -3.15F, 4.35F, 2.20F, 6.30F, new CubeDeformation(0.08F)),
                    PartPose.rotation(0.0F, 0.0F, -0.08F));
        root.getChild("left_arm").addOrReplaceChild("balance_void_left_shoulder_cap",
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-0.20F, -3.40F, -3.15F, 4.35F, 2.20F, 6.30F, new CubeDeformation(0.08F)),
                    PartPose.rotation(0.0F, 0.0F, 0.08F));
    }

    private static void addVoidCrystalKnightDetails(PartDefinition root) {
        root.getChild("body").addOrReplaceChild("true_void_front_upper_left",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.175F, -1.05F, -0.36F, 4.35F, 2.10F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-2.175F, 1.40F, -2.82F, 0.0F, 0.0F, 0.34F));
        root.getChild("body").addOrReplaceChild("true_void_front_upper_right",
                CubeListBuilder.create().texOffs(12, 0)
                        .addBox(-2.175F, -1.05F, -0.36F, 4.35F, 2.10F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(2.175F, 1.40F, -2.82F, 0.0F, 0.0F, -0.34F));
        root.getChild("body").addOrReplaceChild("true_void_front_middle_left",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-2.025F, -0.825F, -0.38F, 4.05F, 1.65F, 0.76F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-2.025F, 3.475F, -2.86F, 0.0F, 0.0F, 0.30F));
        root.getChild("body").addOrReplaceChild("true_void_front_middle_right",
                CubeListBuilder.create().texOffs(36, 0)
                        .addBox(-2.025F, -0.825F, -0.38F, 4.05F, 1.65F, 0.76F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(2.025F, 3.475F, -2.86F, 0.0F, 0.0F, -0.30F));
        root.getChild("body").addOrReplaceChild("true_void_front_lower_left",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-1.775F, -0.725F, -0.36F, 3.55F, 1.45F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-1.775F, 5.375F, -2.84F, 0.0F, 0.0F, 0.26F));
        root.getChild("body").addOrReplaceChild("true_void_front_lower_right",
                CubeListBuilder.create().texOffs(58, 0)
                        .addBox(-1.775F, -0.725F, -0.36F, 3.55F, 1.45F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(1.775F, 5.375F, -2.84F, 0.0F, 0.0F, -0.26F));
        root.getChild("body").addOrReplaceChild("true_void_front_tip",
                CubeListBuilder.create().texOffs(68, 0)
                        .addBox(-1.60F, 6.55F, -3.16F, 3.20F, 0.85F, 0.68F, new CubeDeformation(0.04F)),
                PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("true_void_chest_crystal",
                CubeListBuilder.create().texOffs(82, 0)
                        .addBox(-1.15F, -1.15F, -0.41F, 2.30F, 2.30F, 0.82F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(0.0F, 2.50F, -3.21F, 0.0F, 0.0F, 0.7853982F));
        root.getChild("body").addOrReplaceChild("true_void_back_left",
                CubeListBuilder.create().texOffs(92, 0)
                        .addBox(-2.0F, -1.0F, -0.35F, 4.0F, 2.0F, 0.70F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-2.0F, 1.50F, 2.75F, 0.0F, 0.0F, 0.34F));
        root.getChild("body").addOrReplaceChild("true_void_back_right",
                CubeListBuilder.create().texOffs(104, 0)
                        .addBox(-2.0F, -1.0F, -0.35F, 4.0F, 2.0F, 0.70F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(2.0F, 1.50F, 2.75F, 0.0F, 0.0F, -0.34F));
        root.getChild("body").addOrReplaceChild("true_void_back_middle_left",
                CubeListBuilder.create().texOffs(56, 32)
                        .addBox(-1.85F, -0.775F, -0.36F, 3.70F, 1.55F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-1.85F, 3.55F, 2.78F, 0.0F, 0.0F, 0.28F));
        root.getChild("body").addOrReplaceChild("true_void_back_middle_right",
                CubeListBuilder.create().texOffs(68, 32)
                        .addBox(-1.85F, -0.775F, -0.36F, 3.70F, 1.55F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(1.85F, 3.55F, 2.78F, 0.0F, 0.0F, -0.28F));
        root.getChild("body").addOrReplaceChild("true_void_back_lower_left",
                CubeListBuilder.create().texOffs(80, 32)
                        .addBox(-1.60F, -0.675F, -0.35F, 3.20F, 1.35F, 0.70F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-1.60F, 5.45F, 2.75F, 0.0F, 0.0F, 0.23F));
        root.getChild("body").addOrReplaceChild("true_void_back_lower_right",
                CubeListBuilder.create().texOffs(92, 32)
                        .addBox(-1.60F, -0.675F, -0.35F, 3.20F, 1.35F, 0.70F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(1.60F, 5.45F, 2.75F, 0.0F, 0.0F, -0.23F));
        root.getChild("body").addOrReplaceChild("true_void_back_crystal",
                CubeListBuilder.create().texOffs(116, 0)
                        .addBox(-0.90F, -0.90F, -0.36F, 1.80F, 1.80F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(0.0F, 3.00F, 3.28F, 0.0F, 0.0F, 0.7853982F));
        root.getChild("right_arm").addOrReplaceChild("true_void_right_shoulder_plate",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.05F, -0.625F, -2.675F, 4.10F, 1.25F, 5.35F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(-2.05F, -1.975F, -0.005F, 0.0F, 0.0F, -0.18F));
        root.getChild("right_arm").addOrReplaceChild("true_void_right_shoulder_crystal",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-0.625F, -0.80F, -0.625F, 1.25F, 1.60F, 1.25F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-3.275F, -2.35F, 0.005F, 0.0F, 0.0F, -0.32F));
        root.getChild("left_arm").addOrReplaceChild("true_void_left_shoulder_plate",
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-2.05F, -0.625F, -2.675F, 4.10F, 1.25F, 5.35F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(2.05F, -1.975F, -0.005F, 0.0F, 0.0F, 0.18F));
        root.getChild("left_arm").addOrReplaceChild("true_void_left_shoulder_crystal",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-0.625F, -0.80F, -0.625F, 1.25F, 1.60F, 1.25F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(3.275F, -2.35F, 0.005F, 0.0F, 0.0F, 0.32F));
    }

    private static void addChestDetails(PartDefinition root, TruePathArmorItem.Path path) {
        root.getChild("body").addOrReplaceChild("true_chest_gem",
                CubeListBuilder.create().texOffs(94, 52)
                        .addBox(-1.5F, 2.0F, -3.25F, 3.0F, 3.4F, 1.0F, new CubeDeformation(0.10F)),
                PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("true_right_shoulder",
                CubeListBuilder.create().texOffs(76, 16)
                        .addBox(-4.2F, -3.3F, -3.1F, 4.3F, 2.8F, 6.2F, new CubeDeformation(0.12F)),
                PartPose.rotation(0.0F, 0.0F, -0.08F));
        root.getChild("left_arm").addOrReplaceChild("true_left_shoulder",
                CubeListBuilder.create().texOffs(98, 16)
                        .addBox(-0.1F, -3.3F, -3.1F, 4.3F, 2.8F, 6.2F, new CubeDeformation(0.12F)),
                PartPose.rotation(0.0F, 0.0F, 0.08F));
        if (path == TruePathArmorItem.Path.LIVING) {
            root.getChild("body").addOrReplaceChild("true_living_root_wrap",
                    CubeListBuilder.create().texOffs(108, 56)
                            .addBox(-4.2F, 7.8F, -3.2F, 8.4F, 1.1F, 0.9F, new CubeDeformation(0.06F)),
                    PartPose.rotation(0.0F, 0.0F, 0.18F));
        }
    }

    private static void addLegDetails(PartDefinition root, TruePathArmorItem.Path path) {
        root.getChild("right_leg").addOrReplaceChild("true_right_knee",
                CubeListBuilder.create().texOffs(72, 26)
                        .addBox(-2.3F, 4.7F, -3.1F, 4.6F, 2.3F, 1.2F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("true_left_knee",
                CubeListBuilder.create().texOffs(90, 26)
                        .addBox(-2.3F, 4.7F, -3.1F, 4.6F, 2.3F, 1.2F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        if (path == TruePathArmorItem.Path.CELESTIAL) {
            root.getChild("right_leg").addOrReplaceChild("true_right_shin_star",
                    CubeListBuilder.create().texOffs(110, 26)
                            .addBox(-0.8F, 1.6F, -3.05F, 1.6F, 1.6F, 0.9F, new CubeDeformation(0.06F)),
                    PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("true_left_shin_star",
                    CubeListBuilder.create().texOffs(118, 26)
                            .addBox(-0.8F, 1.6F, -3.05F, 1.6F, 1.6F, 0.9F, new CubeDeformation(0.06F)),
                    PartPose.ZERO);
        }
    }

    private static void addBootDetails(PartDefinition root, TruePathArmorItem.Path path) {
        root.getChild("right_leg").addOrReplaceChild("true_right_toe",
                CubeListBuilder.create().texOffs(72, 48)
                        .addBox(-2.7F, 10.6F, -4.3F, 5.4F, 1.8F, 2.0F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("true_left_toe",
                CubeListBuilder.create().texOffs(94, 48)
                        .addBox(-2.7F, 10.6F, -4.3F, 5.4F, 1.8F, 2.0F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        if (path == TruePathArmorItem.Path.LIVING) {
            root.getChild("right_leg").addOrReplaceChild("true_right_boot_leaf",
                    CubeListBuilder.create().texOffs(116, 48)
                            .addBox(0.6F, 7.2F, -3.25F, 1.6F, 1.1F, 1.0F, new CubeDeformation(0.05F)),
                    PartPose.rotation(0.0F, 0.0F, 0.34F));
            root.getChild("left_leg").addOrReplaceChild("true_left_boot_leaf",
                    CubeListBuilder.create().texOffs(116, 52)
                            .addBox(-2.2F, 7.2F, -3.25F, 1.6F, 1.1F, 1.0F, new CubeDeformation(0.05F)),
                    PartPose.rotation(0.0F, 0.0F, -0.34F));
        }
    }
}
