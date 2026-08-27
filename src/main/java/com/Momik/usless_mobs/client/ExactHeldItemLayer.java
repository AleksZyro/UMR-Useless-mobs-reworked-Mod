package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders held items from anchors defined by the visible exact-mesh pose. */
final class ExactHeldItemLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    private final ItemInHandRenderer itemRenderer;
    private final CustomMob3DModel.Variant variant;
    private final boolean helpingAllay;
    private final ExactRigPose rigPose = new ExactRigPose();

    ExactHeldItemLayer(RenderLayerParent<T, M> parent, ItemInHandRenderer itemRenderer,
                       CustomMob3DModel.Variant variant) {
        super(parent);
        this.itemRenderer = itemRenderer;
        this.variant = variant;
        this.helpingAllay = false;
        if (variant != CustomMob3DModel.Variant.FROST_STRAY) {
            throw new IllegalArgumentException("Missing exact held-item anchor for " + variant);
        }
    }

    ExactHeldItemLayer(RenderLayerParent<T, M> parent, ItemInHandRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
        this.variant = null;
        this.helpingAllay = true;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack item = entity.getMainHandItem();
        if (entity.isInvisible()) {
            return;
        }
        if (item.isEmpty()) {
            return;
        }

        var cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        ExactAnimationLod animationLod = ExactAnimationLod.at(entity.distanceToSqr(cameraPosition));
        float poseAge = animationLod == ExactAnimationLod.FAR
                ? 0.0F : animationLod.quantizedAge(ageInTicks);
        float poseLimbSwing = animationLod == ExactAnimationLod.FAR ? 0.0F : limbSwing;
        float poseLimbAmount = animationLod == ExactAnimationLod.FAR ? 0.0F : limbSwingAmount;
        if (this.helpingAllay) {
            if (!(entity instanceof HelpingAllayEntity allay)) {
                throw new IllegalStateException("Helping Allay item anchor used by " + entity.getType());
            }
            this.rigPose.updateHelpingAllay(poseAge, netHeadYaw, headPitch, allay.action());
        } else {
            this.rigPose.updateFor(this.variant, entity, poseLimbSwing, poseLimbAmount,
                    poseAge, netHeadYaw, headPitch);
        }

        HumanoidArm mainArm = entity.getMainArm();
        boolean leftHand = mainArm == HumanoidArm.LEFT;
        ItemDisplayContext displayContext = leftHand
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

        poseStack.pushPose();
        if (this.helpingAllay) {
            float modelScale = 1.35F;
            poseStack.scale(modelScale, modelScale, modelScale);
            poseStack.translate(0F, 1.5F / modelScale - 1.5F, 0F);
            this.rigPose.helpingAllayMainHand(poseStack, mainArm);
            float itemScale = 0.7F / modelScale;
            poseStack.scale(itemScale, itemScale, itemScale);
        } else {
            switch (this.variant) {
                case FROST_STRAY -> this.rigPose.frostStrayMainHand(poseStack, mainArm);
                default -> throw new IllegalStateException("Missing exact held-item transform for " + this.variant);
            }
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((leftHand ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        this.itemRenderer.renderItem(entity, item, displayContext, leftHand,
                poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
