package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.LivingBatEntity;
import com.Momik.usless_mobs.entity.RootedHuskEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.Set;

/** Animates exact Tripo triangle regions without replacing them with cuboids. */
public final class ExactMobMeshLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    private final ExactMobMesh mesh;
    private final CustomMob3DModel.Variant variant;
    private final ResourceLocation texture;
    private final ExactRigPose rigPose = new ExactRigPose();

    public ExactMobMeshLayer(RenderLayerParent<T, M> parent, ResourceManager resourceManager,
                             CustomMob3DModel.Variant variant, ResourceLocation texture) {
        super(parent);
        this.variant = variant;
        this.texture = texture;
        try {
            this.mesh = ExactMobMesh.load(resourceManager, mobName(variant), expectedBones(variant));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load exact Tripo mesh for " + variant, exception);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(this.texture));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        // Tripo's exported albedo already contains the material's painted light and
        // shadow information. Stable full-bright sampling prevents Minecraft's
        // world light from multiplying that baked appearance a second time.
        int materialLight = LightTexture.FULL_BRIGHT;
        var cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        ExactAnimationLod animationLod = ExactAnimationLod.at(entity.distanceToSqr(cameraPosition));
        boolean animateSurface = animationLod != ExactAnimationLod.FAR;
        float lodAgeInTicks = animationLod.quantizedAge(ageInTicks);
        if (animateSurface) {
            this.rigPose.updateFor(this.variant, entity, limbSwing, limbSwingAmount,
                    lodAgeInTicks, netHeadYaw, headPitch);
        }
        poseStack.pushPose();
        float modelScale = switch (this.variant) {
            case LIVING_BOSS -> 1.35F;
            case WEB_CAVE_SPIDER -> 1.80F;
            case OCTOPUS -> 1.40F;
            case SQUID -> 1.80F;
            case GLOW_SQUID -> 1.15F;
            case LIVING_BAT -> 1.60F;
            case ROOTED_HUSK -> 1.10F;
            default -> 1.0F;
        };
        float floorScaleY = modelScale;
        poseStack.scale(modelScale, modelScale, modelScale);
        float modelYaw = switch (this.variant) {
            case WEB_CAVE_SPIDER -> 180F;
            default -> 0F;
        };
        if (modelYaw != 0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(modelYaw));
        }
        if (entity instanceof OctopusEntity octopus && octopus.isSqueezing()) {
            poseStack.scale(0.49F, 0.55F, 0.49F);
            floorScaleY *= 0.55F;
        }
        poseStack.translate(0F, 1.5F / floorScaleY - 1.5F, 0F);
        if (usesRigidRootAnimation()) {
            // These Tripo assets are continuous surfaces. Their exported regions
            // have no vertex weights, so rotating regions separately tears open
            // shared seams. Animate the complete surface as one cohesive root.
            if (animateSurface) {
                poseStack.translate(0F, rigidRootYOffset(limbSwingAmount), 0F);
            }
        }
        for (String bone : this.mesh.boneNames()) {
            BonePose animation;
            if (animationLod == ExactAnimationLod.FAR || usesRigidRootAnimation()) {
                animation = BonePose.ZERO;
            } else {
                animation = poseFor(bone, entity, limbSwing, limbSwingAmount,
                        lodAgeInTicks, netHeadYaw, headPitch);
            }
            Vector3f pivot = this.mesh.pivot(bone);
            poseStack.pushPose();
            poseStack.translate(
                    (pivot.x() + animation.x()) / 16F,
                    (pivot.y() + animation.y()) / 16F,
                    (pivot.z() + animation.z()) / 16F);
            if (animation.zRot() != 0F) poseStack.mulPose(Axis.ZP.rotation(animation.zRot()));
            if (animation.yRot() != 0F) poseStack.mulPose(Axis.YP.rotation(animation.yRot()));
            if (animation.xRot() != 0F) poseStack.mulPose(Axis.XP.rotation(animation.xRot()));
            poseStack.translate(-pivot.x() / 16F, -pivot.y() / 16F, -pivot.z() / 16F);
            if (!animateSurface) {
                // Keep the exact textured Tripo surface at distance, but skip the
                // allocation-heavy per-vertex deformation that is not visible at
                // this screen size. Nearby entities retain their full animation.
                this.mesh.renderBone(bone, poseStack, buffer, materialLight, overlay);
            } else if (this.variant == CustomMob3DModel.Variant.OCTOPUS
                    && entity instanceof OctopusEntity octopus) {
                this.mesh.renderOctopusBone(bone, poseStack, buffer, materialLight, overlay,
                        lodAgeInTicks, this.rigPose.inWater(), octopus.getActionState(), octopus.isSqueezing());
            } else if (this.variant == CustomMob3DModel.Variant.SQUID) {
                this.mesh.renderSquidBone(bone, poseStack, buffer, materialLight, overlay,
                        lodAgeInTicks, this.rigPose.inWater());
            } else if (this.variant == CustomMob3DModel.Variant.GLOW_SQUID) {
                this.mesh.renderGlowSquidBone(bone, poseStack, buffer, materialLight, overlay,
                        lodAgeInTicks, this.rigPose.inWater());
            } else if (this.variant == CustomMob3DModel.Variant.AXOLOTL) {
                this.mesh.renderAxolotlBone(bone, poseStack, buffer, materialLight, overlay,
                        lodAgeInTicks, limbSwing, limbSwingAmount, this.rigPose.inWater());
            } else if (this.variant == CustomMob3DModel.Variant.OCELOT) {
                this.mesh.renderOcelotBone(bone, poseStack, buffer, materialLight, overlay,
                        lodAgeInTicks, limbSwing, limbSwingAmount, netHeadYaw, headPitch,
                        this.rigPose.sprinting(), !this.rigPose.onGround());
            } else if (this.variant == CustomMob3DModel.Variant.LIVING_BAT
                    && entity instanceof LivingBatEntity bat) {
                this.mesh.renderBatBone(bone, poseStack, buffer, materialLight, overlay,
                        lodAgeInTicks, bat.isResting());
            } else {
                if (this.variant == CustomMob3DModel.Variant.WEB_CAVE_SPIDER) {
                    this.mesh.renderSpiderBone(bone, poseStack, buffer, materialLight, overlay,
                            this.rigPose);
                } else if (this.variant == CustomMob3DModel.Variant.LIVING_BOSS
                        || this.variant == CustomMob3DModel.Variant.POLAR_BEAR) {
                    this.mesh.renderQuadrupedBone(bone, poseStack, buffer, materialLight, overlay,
                            limbSwing, limbSwingAmount, netHeadYaw, headPitch);
                } else if (this.variant == CustomMob3DModel.Variant.FROST_STRAY) {
                    this.mesh.renderFrostStrayBone(bone, poseStack, buffer,
                            materialLight, overlay, this.rigPose);
                } else if (this.variant == CustomMob3DModel.Variant.CORAL_DROWNED
                        || this.variant == CustomMob3DModel.Variant.WITCH_BOSS
                        || this.variant == CustomMob3DModel.Variant.ROOTED_HUSK) {
                    boolean aimingBow = entity instanceof AbstractSkeleton skeleton && skeleton.isAggressive();
                    boolean aggressiveMelee = entity instanceof CoralDrownedEntity drowned && drowned.isAggressive()
                            || entity instanceof RootedHuskEntity husk && husk.isAggressive();
                    this.mesh.renderHumanoidBone(bone, poseStack, buffer, materialLight, overlay,
                            limbSwing, limbSwingAmount, lodAgeInTicks, netHeadYaw, headPitch,
                            this.rigPose.inWater(), aimingBow, aggressiveMelee);
                } else {
                    this.mesh.renderBone(bone, poseStack, buffer, materialLight, overlay);
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private boolean usesRigidRootAnimation() {
        return switch (this.variant) {
            case LIVING_BOSS, WEB_CAVE_SPIDER, OCTOPUS, SQUID, GLOW_SQUID,
                    WITCH_BOSS, LIVING_BAT, ROOTED_HUSK, POLAR_BEAR, FROST_STRAY,
                    CORAL_DROWNED -> true;
            case AXOLOTL, OCELOT -> true;
            default -> false;
        };
    }

    private float rigidRootYOffset(float amount) {
        return switch (this.variant) {
            case LIVING_BOSS -> Mth.abs(this.rigPose.walkCos()) * 0.030F * amount;
            case WEB_CAVE_SPIDER -> this.rigPose.rootSin() * 0.011F;
            case OCTOPUS -> this.rigPose.rootSin() * (this.rigPose.inWater() ? 0.019F : 0.006F);
            case SQUID -> this.rigPose.rootSin() * 0.012F;
            case GLOW_SQUID -> this.rigPose.rootSin() * 0.018F;
            case WITCH_BOSS -> this.rigPose.rootSin() * 0.008F;
            case LIVING_BAT -> this.rigPose.rootSin() * 0.015F;
            case ROOTED_HUSK -> this.rigPose.rootSin() * 0.004F;
            case POLAR_BEAR -> Mth.abs(this.rigPose.walkCos()) * 0.018F * amount;
            case FROST_STRAY -> Mth.abs(this.rigPose.walkCos()) * 0.014F * amount;
            case CORAL_DROWNED -> this.rigPose.rootSin() * (this.rigPose.inWater() ? 0.018F : 0.006F);
            case AXOLOTL -> this.rigPose.rootSin() * (this.rigPose.inWater() ? 0.012F : 0.004F);
            case OCELOT -> Mth.abs(this.rigPose.walkCos()) * 0.012F * amount;
            default -> 0F;
        };
    }

    private BonePose poseFor(String bone, T entity, float limbSwing, float amount,
                             float age, float netHeadYaw, float headPitch) {
        float walk = limbSwing * 0.6662F;
        if (this.variant == CustomMob3DModel.Variant.OCTOPUS
                && entity instanceof OctopusEntity octopus) {
            BonePose octopusAnimation = octopusPose(bone, octopus, age);
            if (octopusAnimation != null) {
                return octopusAnimation;
            }
        }
        if (bone.equals("head")) {
            float sway = this.variant == CustomMob3DModel.Variant.WITCH_BOSS
                    ? Mth.sin(age * 0.05F) * 0.08F : 0F;
            return new BonePose(0, 0, 0, headPitch * Mth.DEG_TO_RAD,
                    netHeadYaw * Mth.DEG_TO_RAD + sway, 0);
        }
        if (bone.equals("right_arm")) {
            return BonePose.rotationX(Mth.cos(walk + Mth.PI) * 1.25F * amount);
        }
        if (bone.equals("left_arm")) {
            return BonePose.rotationX(Mth.cos(walk) * 1.25F * amount);
        }
        if (bone.equals("right_leg")) {
            return BonePose.rotationX(Mth.cos(walk) * 1.15F * amount);
        }
        if (bone.equals("left_leg")) {
            return BonePose.rotationX(Mth.cos(walk + Mth.PI) * 1.15F * amount);
        }
        if (bone.startsWith("leg_")) {
            boolean alternate = bone.contains("front_left") || bone.contains("rear_right");
            float phase = alternate ? 0F : Mth.PI;
            return BonePose.rotationX(Mth.cos(walk * 1.35F + phase) * 0.78F * amount);
        }
        if (bone.startsWith("web_leg_")) {
            int index = Integer.parseInt(bone.substring("web_leg_".length()));
            float phase = (index % 4) * 0.62F + (index >= 4 ? Mth.PI : 0F);
            float side = index < 4 ? -1F : 1F;
            return new BonePose(0, 0, 0,
                    0,
                    Mth.cos(limbSwing * 1.45F + phase) * 0.62F * amount,
                    side * (0.12F + Mth.sin(limbSwing * 1.45F + phase) * 0.28F * amount));
        }
        if (bone.startsWith("tentacle")) {
            int index = Integer.parseInt(bone.substring("tentacle".length()));
            float strength = entity.isInWater() ? 1F : 0.32F;
            float phase = index * Mth.PI / 4F;
            return new BonePose(0, 0, 0,
                    Mth.sin(age * 0.20F + phase) * 0.38F * strength,
                    0,
                    Mth.cos(age * 0.14F + phase) * 0.27F * strength);
        }
        if (this.variant == CustomMob3DModel.Variant.SQUID
                && (bone.startsWith("arm") || bone.startsWith("catching_tentacle"))) {
            boolean catching = bone.startsWith("catching_tentacle");
            int index = Integer.parseInt(bone.substring(catching ? "catching_tentacle".length() : "arm".length()));
            float phase = index * Mth.PI / (catching ? 2.0F : 4.0F);
            float waterStrength = entity.isInWater() ? 1.0F : 0.22F;
            float reach = catching ? 0.24F : 0.14F;
            return new BonePose(0, 0, 0,
                    Mth.sin(age * 0.24F + phase) * reach * waterStrength,
                    Mth.cos(age * 0.19F + phase) * reach * 0.72F * waterStrength,
                    0);
        }
        if (bone.contains("wing")) {
            boolean left = bone.startsWith("left");
            boolean tip = bone.endsWith("tip");
            float side = left ? -1F : 1F;
            float flap = Mth.cos(age * 0.78F) * (tip ? 0.42F : 0.72F);
            return new BonePose(0, 0, 0, 0, 0, side * (0.18F + flap));
        }
        if (bone.equals("body")) {
            return switch (this.variant) {
                case LIVING_BOSS -> new BonePose(0, Mth.abs(Mth.cos(walk)) * 0.50F * amount, 0, 0, 0, 0);
                case WEB_CAVE_SPIDER -> new BonePose(0, Mth.sin(age * 0.20F) * 0.18F, 0, 0, 0, 0);
                case OCTOPUS -> new BonePose(0, Mth.sin(age * 0.08F) * (entity.isInWater() ? 0.30F : 0.10F), 0, 0, 0, 0);
                case SQUID -> new BonePose(0, Mth.sin(age * 0.12F) * 0.18F, 0, 0, 0, Mth.sin(age * 0.07F) * 0.025F);
                case WITCH_BOSS -> new BonePose(0, Mth.sin(age * 0.10F) * 0.12F, 0, 0, 0, Mth.sin(age * 0.06F) * 0.05F);
                case LIVING_BAT -> new BonePose(0, Mth.sin(age * 0.16F) * 0.24F, 0, 0, 0, 0);
                case ROOTED_HUSK -> new BonePose(0, 0, 0, 0, 0, Mth.sin(age * 0.04F) * 0.035F);
                default -> BonePose.ZERO;
            };
        }
        return BonePose.ZERO;
    }

    private static BonePose octopusPose(String bone, OctopusEntity octopus, float age) {
        byte action = octopus.getActionState();
        float waterStrength = octopus.isInWater() ? 1.0F : 0.30F;
        if (bone.equals("body")) {
            float bob = switch (action) {
                case OctopusEntity.ACTION_AMBUSH, OctopusEntity.ACTION_CAMOUFLAGE ->
                        Mth.sin(age * 0.055F) * 0.06F;
                case OctopusEntity.ACTION_INK -> Mth.sin(age * 0.75F) * 0.26F;
                case OctopusEntity.ACTION_SWIM -> Mth.sin(age * 0.18F) * 0.32F;
                case OctopusEntity.ACTION_GRAB, OctopusEntity.ACTION_OBJECT ->
                        Mth.sin(age * 0.10F) * 0.14F;
                default -> Mth.sin(age * 0.08F) * 0.18F;
            };
            return new BonePose(0, bob * waterStrength, 0, 0, 0, 0);
        }
        if (!bone.startsWith("tentacle")) {
            return null;
        }

        int index = Integer.parseInt(bone.substring("tentacle".length()));
        float phase = index * Mth.PI / 4F;
        float side = index < 4 ? -1F : 1F;
        float xRot;
        float zRot;
        switch (action) {
            case OctopusEntity.ACTION_SWIM -> {
                float stroke = Mth.sin(age * 0.32F + phase);
                xRot = (stroke * 0.58F - 0.10F) * waterStrength;
                zRot = Mth.cos(age * 0.24F + phase) * 0.34F * waterStrength;
            }
            case OctopusEntity.ACTION_AMBUSH -> {
                xRot = 0.58F + Mth.sin(age * 0.08F + phase) * 0.06F;
                zRot = side * 0.34F;
            }
            case OctopusEntity.ACTION_GRAB -> {
                boolean reachingArm = index == 0 || index == 7;
                xRot = reachingArm ? -0.82F + Mth.sin(age * 0.42F) * 0.16F : 0.28F;
                zRot = reachingArm ? side * 0.10F : side * 0.32F;
            }
            case OctopusEntity.ACTION_INK -> {
                float pulse = Mth.sin(age * 0.72F);
                xRot = -0.28F - Mth.abs(pulse) * 0.38F;
                zRot = side * (0.36F + Mth.abs(pulse) * 0.24F);
            }
            case OctopusEntity.ACTION_CAMOUFLAGE -> {
                xRot = Mth.sin(age * 0.045F + phase) * 0.035F;
                zRot = Mth.cos(age * 0.040F + phase) * 0.025F;
            }
            case OctopusEntity.ACTION_OBJECT -> {
                boolean holdingArm = index == 0;
                xRot = holdingArm ? 0.92F : Mth.sin(age * 0.12F + phase) * 0.16F;
                zRot = holdingArm ? -0.30F : Mth.cos(age * 0.10F + phase) * 0.12F;
            }
            default -> {
                xRot = Mth.sin(age * 0.18F + phase) * 0.32F * waterStrength;
                zRot = Mth.cos(age * 0.13F + phase) * 0.24F * waterStrength;
            }
        }
        if (octopus.isSqueezing()) {
            xRot += 0.52F;
            zRot += side * 0.20F;
        }
        return new BonePose(0, 0, 0, xRot, 0, zRot);
    }

    private static String mobName(CustomMob3DModel.Variant variant) {
        return switch (variant) {
            case LIVING_BOSS -> "living_boss";
            case WEB_CAVE_SPIDER -> "web_cave_spider";
            case OCTOPUS -> "octopus";
            case SQUID -> "squid";
            case GLOW_SQUID -> "glow_squid";
            case WITCH_BOSS -> "witch_boss";
            case LIVING_BAT -> "living_bat";
            case ROOTED_HUSK -> "rooted_husk";
            case POLAR_BEAR -> "polar_bear";
            case FROST_STRAY -> "frost_stray";
            case CORAL_DROWNED -> "coral_drowned";
            case AXOLOTL -> "axolotl";
            case OCELOT -> "ocelot";
            default -> throw new IllegalArgumentException("Variant is not an approved exact Tripo mob: " + variant);
        };
    }

    private static Set<String> expectedBones(CustomMob3DModel.Variant variant) {
        return switch (variant) {
            case LIVING_BOSS -> Set.of("body", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right");
            case WEB_CAVE_SPIDER -> Set.of("body", "web_leg_0", "web_leg_1", "web_leg_2", "web_leg_3", "web_leg_4", "web_leg_5", "web_leg_6", "web_leg_7");
            case OCTOPUS -> Set.of("body", "tentacle0", "tentacle1", "tentacle2", "tentacle3", "tentacle4", "tentacle5", "tentacle6", "tentacle7");
            case SQUID -> Set.of("body", "arm0", "arm1", "arm2", "arm3", "arm4", "arm5", "arm6", "arm7", "catching_tentacle0", "catching_tentacle1");
            case GLOW_SQUID -> Set.of("body", "arm0", "arm1", "arm2", "arm3", "arm4", "arm5", "arm6", "arm7", "catching_tentacle0", "catching_tentacle1");
            case WITCH_BOSS, ROOTED_HUSK -> Set.of("body", "head", "right_arm", "left_arm", "right_leg", "left_leg");
            case LIVING_BAT -> Set.of("body", "head", "right_wing", "left_wing", "right_wing_tip", "left_wing_tip");
            case POLAR_BEAR -> Set.of("body", "head", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right");
            case FROST_STRAY -> Set.of("body", "head", "right_arm", "left_arm", "right_leg", "left_leg");
            case CORAL_DROWNED -> Set.of("body", "head", "right_arm", "left_arm", "right_leg", "left_leg");
            case AXOLOTL -> Set.of("body", "head", "tail", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right");
            case OCELOT -> Set.of("body", "head", "tail", "leg_front_left", "leg_front_right", "leg_rear_left", "leg_rear_right");
            default -> throw new IllegalArgumentException("Variant is not an approved exact Tripo mob: " + variant);
        };
    }

    private record BonePose(float x, float y, float z, float xRot, float yRot, float zRot) {
        private static final BonePose ZERO = new BonePose(0, 0, 0, 0, 0, 0);

        private static BonePose rotationX(float angle) {
            return new BonePose(0, 0, 0, angle, 0, 0);
        }
    }
}
