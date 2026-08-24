package com.Momik.usless_mobs.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.api.SlotContext;

public class KingSlimeCrownCurioRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack matrixStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        LivingEntity entity = slotContext.entity();
        matrixStack.pushPose();

        // Align with head bone so the crown follows head rotation (look up/down/around).
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid) {
            humanoid.head.translateAndRotate(matrixStack);
        }

        // Flip back upright (vanilla entity render has Y-axis inverted).
        // The 3D model's display.head transform handles positioning/scale from here.
        matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        boolean hasHelmetUnderCrown = !helmet.isEmpty()
                && !helmet.is(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get())
                && !helmet.is(com.Momik.usless_mobs.registry.ModItems.NETHERITE_KINGS_KRONE.get());
        // -0.385 sits directly on the head. With a helmet underneath, lift by ~2 px (-0.25).
        double yOffset = hasHelmetUnderCrown ? -0.25D : -0.385D;
        matrixStack.translate(0.0D, yOffset, 0.0D);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY,
                matrixStack, renderTypeBuffer, entity.level(), entity.getId());

        matrixStack.popPose();
    }
}
