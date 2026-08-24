package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KingCrownLayer extends RenderLayer<Slime, SlimeModel<Slime>> {

    public KingCrownLayer(RenderLayerParent<Slime, SlimeModel<Slime>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Slime slime, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(slime instanceof KingSlimeEntity king)) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack crownStack = new ItemStack(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get());

        poseStack.pushPose();

        // SlimeRenderer leaves the pose flipped upside-down (vanilla entity-model convention:
        // Y axis points DOWN). Crown was rendering inverted — flip back via 180° around Z,
        // then translate UP (which is +Y in the now-corrected space).
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));

        int size = king.getSize();
        float topY = 0.7F + size * 0.06F;
        poseStack.translate(0.0F, topY, 0.0F);

        // Soft spin for that floating-crown effect
        float yaw = (ageInTicks + partialTicks) * 1.5F;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));

        // Scale relative to King size
        float crownScale = 0.10F + size * 0.04F;
        poseStack.scale(crownScale, crownScale, crownScale);

        itemRenderer.renderStatic(crownStack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, slime.level(), slime.getId());

        poseStack.popPose();
    }
}
