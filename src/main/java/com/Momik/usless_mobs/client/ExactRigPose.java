package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;

/** Reusable primitive scratch state shared by exact-mesh pose calculations. */
final class ExactRigPose {
    private float walkCos;
    private float rootSin;
    private boolean inWater;
    private boolean onGround;
    private boolean sprinting;
    private float rightShoulderAngle;
    private float leftShoulderAngle;
    private float rightShoulderYaw;
    private float leftShoulderYaw;
    private float rightElbowAngle;
    private float leftElbowAngle;
    private float rightHipAngle;
    private float leftHipAngle;
    private float rightKneeAngle;
    private float leftKneeAngle;
    private float targetYaw;
    private float targetPitch;
    private float upperBodyYaw;
    private float upperBodyPitch;
    private float bowBlend;
    private float rightShoulderCos;
    private float rightShoulderSin;
    private float leftShoulderCos;
    private float leftShoulderSin;
    private float rightShoulderYawCos;
    private float rightShoulderYawSin;
    private float leftShoulderYawCos;
    private float leftShoulderYawSin;
    private float rightElbowCos;
    private float rightElbowSin;
    private float leftElbowCos;
    private float leftElbowSin;
    private float rightHipCos;
    private float rightHipSin;
    private float leftHipCos;
    private float leftHipSin;
    private float rightKneeCos;
    private float rightKneeSin;
    private float leftKneeCos;
    private float leftKneeSin;
    private float targetYawCos;
    private float targetYawSin;
    private float targetPitchCos;
    private float targetPitchSin;
    private float upperBodyYawCos;
    private float upperBodyYawSin;
    private float upperBodyPitchCos;
    private float upperBodyPitchSin;
    private float spiderStepCosA;
    private float spiderStepCosB;
    private float spiderSweepSinA;
    private float spiderSweepSinB;
    private float rightWingRoot;
    private float leftWingRoot;
    private float rightWingTip;
    private float leftWingTip;
    private float rightAllayArm;
    private float leftAllayArm;
    private float allayHeadYaw;
    private float allayHeadPitch;
    private float allayCorePulse;
    private float rightWingRootCos;
    private float rightWingRootSin;
    private float leftWingRootCos;
    private float leftWingRootSin;
    private float rightWingTipCos;
    private float rightWingTipSin;
    private float leftWingTipCos;
    private float leftWingTipSin;
    private float rightAllayArmCos;
    private float rightAllayArmSin;
    private float leftAllayArmCos;
    private float leftAllayArmSin;
    private float allayHeadYawCos;
    private float allayHeadYawSin;
    private float allayHeadPitchCos;
    private float allayHeadPitchSin;

    void updateFor(CustomMob3DModel.Variant variant, LivingEntity entity,
                   float limbSwing, float limbSwingAmount, float ageInTicks,
                   float netHeadYaw, float headPitch) {
        if (variant == null || entity == null) {
            throw new IllegalArgumentException("variant and entity are required");
        }
        float walk = limbSwing * 0.6662F;
        float rootSpeed = switch (variant) {
            case WEB_CAVE_SPIDER -> 0.20F;
            case OCTOPUS -> 0.08F;
            case SQUID -> 0.12F;
            case GLOW_SQUID -> 0.14F;
            case WITCH_BOSS, CORAL_DROWNED -> 0.10F;
            case LIVING_BAT, AXOLOTL -> 0.16F;
            case ROOTED_HUSK -> 0.08F;
            case LIVING_BOSS, FROST_STRAY, POLAR_BEAR, OCELOT -> 0.0F;
        };
        this.walkCos = Mth.cos(walk);
        this.rootSin = Mth.sin(ageInTicks * rootSpeed);
        this.inWater = entity.isInWater();
        this.onGround = entity.onGround();
        this.sprinting = entity.isSprinting();
        switch (variant) {
            case WEB_CAVE_SPIDER -> updateWebCaveSpider(limbSwing, limbSwingAmount);
            case FROST_STRAY -> updateFrostStray(limbSwing, limbSwingAmount, ageInTicks,
                    netHeadYaw, headPitch,
                    entity instanceof AbstractSkeleton skeleton && skeleton.isAggressive(),
                    entity.getTicksUsingItem());
            default -> {
            }
        }
    }

    private void updateWebCaveSpider(float limbSwing, float limbSwingAmount) {
        float gait = limbSwing * 1.45F;
        this.spiderStepCosA = Mth.cos(gait) * limbSwingAmount;
        this.spiderStepCosB = Mth.cos(gait + Mth.PI) * limbSwingAmount;
        this.spiderSweepSinA = Mth.sin(gait) * limbSwingAmount;
        this.spiderSweepSinB = Mth.sin(gait + Mth.PI) * limbSwingAmount;
    }

    void updateHelpingAllay(float ageInTicks, float netHeadYaw, float headPitch,
                            byte actionState) {
        float flapSpeed = actionState == HelpingAllayEntity.ACTION_TELEPORT ? 1.34F
                : actionState == HelpingAllayEntity.ACTION_SHIELD
                || actionState == HelpingAllayEntity.ACTION_HEAL ? 1.05F : 0.78F;
        float flapStrength = actionState == HelpingAllayEntity.ACTION_TELEPORT ? 1.05F
                : actionState == HelpingAllayEntity.ACTION_SHIELD
                || actionState == HelpingAllayEntity.ACTION_HEAL ? 0.86F : 0.62F;
        float rootFlap = Mth.sin(ageInTicks * flapSpeed) * flapStrength;
        float delayedTipFlap = Mth.sin(ageInTicks * flapSpeed - 0.62F) * flapStrength * 0.54F;
        this.rightWingRoot = -rootFlap;
        this.leftWingRoot = rootFlap;
        this.rightWingTip = -delayedTipFlap;
        this.leftWingTip = delayedTipFlap;

        if (actionState == HelpingAllayEntity.ACTION_REVEAL) {
            this.rightAllayArm = -1.08F;
            this.leftAllayArm = -0.92F;
        } else if (actionState == HelpingAllayEntity.ACTION_SHIELD) {
            this.rightAllayArm = -1.30F;
            this.leftAllayArm = -1.18F;
        } else if (actionState == HelpingAllayEntity.ACTION_HEAL) {
            this.rightAllayArm = -0.78F;
            this.leftAllayArm = -0.64F;
        } else if (actionState == HelpingAllayEntity.ACTION_BOND) {
            this.rightAllayArm = -0.62F;
            this.leftAllayArm = -0.82F;
        } else if (actionState == HelpingAllayEntity.ACTION_TELEPORT) {
            this.rightAllayArm = 0.34F;
            this.leftAllayArm = 0.34F;
        } else {
            float idleArm = Mth.sin(ageInTicks * 0.10F) * 0.12F;
            this.rightAllayArm = -idleArm;
            this.leftAllayArm = idleArm;
        }

        this.allayHeadYaw = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.55F, 0.55F);
        this.allayHeadPitch = Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.42F, 0.42F);
        float coreStrength = actionState == HelpingAllayEntity.ACTION_HEAL ? 0.30F
                : actionState == HelpingAllayEntity.ACTION_BOND ? 0.20F : 0.08F;
        this.allayCorePulse = Mth.sin(ageInTicks * 0.42F) * coreStrength;

        this.rightWingRootCos = Mth.cos(this.rightWingRoot);
        this.rightWingRootSin = Mth.sin(this.rightWingRoot);
        this.leftWingRootCos = Mth.cos(this.leftWingRoot);
        this.leftWingRootSin = Mth.sin(this.leftWingRoot);
        this.rightWingTipCos = Mth.cos(this.rightWingTip);
        this.rightWingTipSin = Mth.sin(this.rightWingTip);
        this.leftWingTipCos = Mth.cos(this.leftWingTip);
        this.leftWingTipSin = Mth.sin(this.leftWingTip);
        this.rightAllayArmCos = Mth.cos(this.rightAllayArm);
        this.rightAllayArmSin = Mth.sin(this.rightAllayArm);
        this.leftAllayArmCos = Mth.cos(this.leftAllayArm);
        this.leftAllayArmSin = Mth.sin(this.leftAllayArm);
        this.allayHeadYawCos = Mth.cos(this.allayHeadYaw);
        this.allayHeadYawSin = Mth.sin(this.allayHeadYaw);
        this.allayHeadPitchCos = Mth.cos(this.allayHeadPitch);
        this.allayHeadPitchSin = Mth.sin(this.allayHeadPitch);
    }

    private void updateFrostStray(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, boolean aimingBow,
                                  int itemUseTicks) {
        float walk = limbSwing * 0.6662F;
        float rightWalkPhase = Mth.cos(walk + Mth.PI);
        float leftWalkPhase = Mth.cos(walk);
        float drawProgress = aimingBow
                ? Mth.clamp((itemUseTicks + 1.0F) / 6.0F, 0.0F, 1.0F) : 0.0F;
        this.bowBlend = drawProgress * drawProgress * (3.0F - 2.0F * drawProgress);

        this.targetYaw = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.70F, 0.70F);
        this.targetPitch = Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.55F, 0.55F);
        this.upperBodyYaw = this.targetYaw * 0.36F * this.bowBlend;
        this.upperBodyPitch = this.targetPitch * 0.24F * this.bowBlend;

        float normalRightShoulder = leftWalkPhase * 0.82F * limbSwingAmount;
        float normalLeftShoulder = rightWalkPhase * 0.82F * limbSwingAmount;
        this.rightShoulderAngle = Mth.lerp(this.bowBlend, normalRightShoulder,
                -1.47F + this.targetPitch * 0.72F);
        this.leftShoulderAngle = Mth.lerp(this.bowBlend, normalLeftShoulder,
                -1.30F + this.targetPitch * 0.72F);
        this.rightShoulderYaw = Mth.lerp(this.bowBlend, 0.0F,
                this.targetYaw - 0.18F);
        this.leftShoulderYaw = Mth.lerp(this.bowBlend, 0.0F,
                this.targetYaw + 0.52F);
        this.rightElbowAngle = Mth.lerp(this.bowBlend, 0.0F, -0.16F);
        this.leftElbowAngle = Mth.lerp(this.bowBlend, 0.0F, 0.48F);

        this.rightHipAngle = rightWalkPhase * 0.88F * limbSwingAmount;
        this.leftHipAngle = leftWalkPhase * 0.88F * limbSwingAmount;
        this.rightKneeAngle = Math.max(0.0F, -rightWalkPhase) * 0.34F * limbSwingAmount;
        this.leftKneeAngle = Math.max(0.0F, -leftWalkPhase) * 0.34F * limbSwingAmount;

        this.rightShoulderCos = Mth.cos(this.rightShoulderAngle);
        this.rightShoulderSin = Mth.sin(this.rightShoulderAngle);
        this.leftShoulderCos = Mth.cos(this.leftShoulderAngle);
        this.leftShoulderSin = Mth.sin(this.leftShoulderAngle);
        this.rightShoulderYawCos = Mth.cos(this.rightShoulderYaw);
        this.rightShoulderYawSin = Mth.sin(this.rightShoulderYaw);
        this.leftShoulderYawCos = Mth.cos(this.leftShoulderYaw);
        this.leftShoulderYawSin = Mth.sin(this.leftShoulderYaw);
        this.rightElbowCos = Mth.cos(this.rightElbowAngle);
        this.rightElbowSin = Mth.sin(this.rightElbowAngle);
        this.leftElbowCos = Mth.cos(this.leftElbowAngle);
        this.leftElbowSin = Mth.sin(this.leftElbowAngle);
        this.rightHipCos = Mth.cos(this.rightHipAngle);
        this.rightHipSin = Mth.sin(this.rightHipAngle);
        this.leftHipCos = Mth.cos(this.leftHipAngle);
        this.leftHipSin = Mth.sin(this.leftHipAngle);
        this.rightKneeCos = Mth.cos(this.rightKneeAngle);
        this.rightKneeSin = Mth.sin(this.rightKneeAngle);
        this.leftKneeCos = Mth.cos(this.leftKneeAngle);
        this.leftKneeSin = Mth.sin(this.leftKneeAngle);
        this.targetYawCos = Mth.cos(this.targetYaw);
        this.targetYawSin = Mth.sin(this.targetYaw);
        this.targetPitchCos = Mth.cos(this.targetPitch);
        this.targetPitchSin = Mth.sin(this.targetPitch);
        this.upperBodyYawCos = Mth.cos(this.upperBodyYaw);
        this.upperBodyYawSin = Mth.sin(this.upperBodyYaw);
        this.upperBodyPitchCos = Mth.cos(this.upperBodyPitch);
        this.upperBodyPitchSin = Mth.sin(this.upperBodyPitch);
    }

    void frostStrayMainHand(PoseStack poseStack, HumanoidArm arm) {
        boolean left = arm == HumanoidArm.LEFT;
        float side = left ? -1.0F : 1.0F;
        poseStack.translate(0.0F, 0.42F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(this.upperBodyYaw));
        poseStack.mulPose(Axis.XP.rotation(this.upperBodyPitch));
        poseStack.translate(0.0F, -0.42F, 0.0F);
        poseStack.translate(side * 0.2403F, 0.252F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(left ? this.leftShoulderYaw : this.rightShoulderYaw));
        poseStack.mulPose(Axis.XP.rotation(left ? this.leftShoulderAngle : this.rightShoulderAngle));
        poseStack.translate(-side * 0.2403F, -0.252F, 0.0F);
        poseStack.translate(side * 0.375F, 0.54F, 0.0F);
        poseStack.mulPose(Axis.XP.rotation(left ? this.leftElbowAngle : this.rightElbowAngle));
        poseStack.translate(-side * 0.375F, -0.54F, 0.0F);
        poseStack.translate(side * 0.46F, 0.78F, 0.0F);
    }

    void helpingAllayMainHand(PoseStack poseStack, HumanoidArm arm) {
        boolean left = arm == HumanoidArm.LEFT;
        float pivotX = left ? 0.15F : -0.15F;
        poseStack.translate(pivotX, 1.053F, 0.0F);
        poseStack.mulPose(Axis.XP.rotation(left ? this.leftAllayArm : this.rightAllayArm));
        poseStack.translate(-pivotX, -1.053F, 0.0F);
        poseStack.translate(left ? 0.185F : -0.185F, 1.285F, 0.035F);
    }

    float walkCos() {
        return this.walkCos;
    }

    float rootSin() {
        return this.rootSin;
    }

    boolean inWater() {
        return this.inWater;
    }

    boolean onGround() {
        return this.onGround;
    }

    boolean sprinting() {
        return this.sprinting;
    }

    float rightShoulderCos() { return this.rightShoulderCos; }
    float rightShoulderSin() { return this.rightShoulderSin; }
    float leftShoulderCos() { return this.leftShoulderCos; }
    float leftShoulderSin() { return this.leftShoulderSin; }
    float rightShoulderYawCos() { return this.rightShoulderYawCos; }
    float rightShoulderYawSin() { return this.rightShoulderYawSin; }
    float leftShoulderYawCos() { return this.leftShoulderYawCos; }
    float leftShoulderYawSin() { return this.leftShoulderYawSin; }
    float rightElbowCos() { return this.rightElbowCos; }
    float rightElbowSin() { return this.rightElbowSin; }
    float leftElbowCos() { return this.leftElbowCos; }
    float leftElbowSin() { return this.leftElbowSin; }
    float rightHipCos() { return this.rightHipCos; }
    float rightHipSin() { return this.rightHipSin; }
    float leftHipCos() { return this.leftHipCos; }
    float leftHipSin() { return this.leftHipSin; }
    float rightKneeCos() { return this.rightKneeCos; }
    float rightKneeSin() { return this.rightKneeSin; }
    float leftKneeCos() { return this.leftKneeCos; }
    float leftKneeSin() { return this.leftKneeSin; }
    float targetYawCos() { return this.targetYawCos; }
    float targetYawSin() { return this.targetYawSin; }
    float targetPitchCos() { return this.targetPitchCos; }
    float targetPitchSin() { return this.targetPitchSin; }
    float upperBodyYawCos() { return this.upperBodyYawCos; }
    float upperBodyYawSin() { return this.upperBodyYawSin; }
    float upperBodyPitchCos() { return this.upperBodyPitchCos; }
    float upperBodyPitchSin() { return this.upperBodyPitchSin; }
    float spiderStepCosA() { return this.spiderStepCosA; }
    float spiderStepCosB() { return this.spiderStepCosB; }
    float spiderSweepSinA() { return this.spiderSweepSinA; }
    float spiderSweepSinB() { return this.spiderSweepSinB; }
    float rightWingRootCos() { return this.rightWingRootCos; }
    float rightWingRootSin() { return this.rightWingRootSin; }
    float leftWingRootCos() { return this.leftWingRootCos; }
    float leftWingRootSin() { return this.leftWingRootSin; }
    float rightWingTipCos() { return this.rightWingTipCos; }
    float rightWingTipSin() { return this.rightWingTipSin; }
    float leftWingTipCos() { return this.leftWingTipCos; }
    float leftWingTipSin() { return this.leftWingTipSin; }
    float rightAllayArmCos() { return this.rightAllayArmCos; }
    float rightAllayArmSin() { return this.rightAllayArmSin; }
    float leftAllayArmCos() { return this.leftAllayArmCos; }
    float leftAllayArmSin() { return this.leftAllayArmSin; }
    float allayHeadYawCos() { return this.allayHeadYawCos; }
    float allayHeadYawSin() { return this.allayHeadYawSin; }
    float allayHeadPitchCos() { return this.allayHeadPitchCos; }
    float allayHeadPitchSin() { return this.allayHeadPitchSin; }
    float allayCorePulse() { return this.allayCorePulse; }
}
