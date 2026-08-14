package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SlimeEquipmentLayer extends RenderLayer<Slime, SlimeModel<Slime>> {

    private static final int MIN_SLIME_SIZE_FOR_EQUIPMENT = 3;

    public SlimeEquipmentLayer(RenderLayerParent<Slime, SlimeModel<Slime>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Slime slime, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(slime instanceof BlueSlimeEntity)) {
            return;
        }
        if (slime.getSize() < MIN_SLIME_SIZE_FOR_EQUIPMENT) {
            return;
        }

        ItemStack mainhand = slime.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack helmet   = slime.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest    = slime.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs     = slime.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet     = slime.getItemBySlot(EquipmentSlot.FEET);

        if (mainhand.isEmpty() && helmet.isEmpty() && chest.isEmpty() && legs.isEmpty() && feet.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        // Sanfte Drehung des Item-Displays
        float yaw = (ageInTicks + partialTicks) * 2.0F;

        // Items sind innerhalb des Slime-Koerpers verteilt
        // Slime-Modell ist um die Y-Achse zentriert mit Hoehe ~1.0 / Size
        // Wir verteilen die Items vertikal innerhalb des Koerpers
        renderItem(poseStack, bufferSource, packedLight, itemRenderer, slime, helmet,   0.0F,  0.70F, 0.0F, yaw);
        renderItem(poseStack, bufferSource, packedLight, itemRenderer, slime, chest,    0.0F,  0.40F, 0.0F, yaw + 60.0F);
        renderItem(poseStack, bufferSource, packedLight, itemRenderer, slime, legs,     0.0F,  0.10F, 0.0F, yaw + 120.0F);
        renderItem(poseStack, bufferSource, packedLight, itemRenderer, slime, feet,     0.0F, -0.20F, 0.0F, yaw + 180.0F);
        renderItem(poseStack, bufferSource, packedLight, itemRenderer, slime, mainhand, 0.3F,  0.50F, 0.0F, yaw + 240.0F);
    }

    private void renderItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemRenderer renderer, Slime slime, ItemStack stack, float xOffset, float yOffset, float zOffset, float yawDegrees) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, zOffset);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawDegrees));

        float scale = 0.55F;
        poseStack.scale(scale, scale, scale);

        renderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, bufferSource, slime.level(), slime.getId());

        poseStack.popPose();
    }
}
