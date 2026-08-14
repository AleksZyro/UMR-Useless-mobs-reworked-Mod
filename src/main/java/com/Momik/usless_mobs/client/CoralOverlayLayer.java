package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Drowned;

public class CoralOverlayLayer extends RenderLayer<Drowned, DrownedModel<Drowned>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/ocean/staged/coral_drowned_outer_layer.png");

    public CoralOverlayLayer(RenderLayerParent<Drowned, DrownedModel<Drowned>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Drowned entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof CoralDrownedEntity)) {
            return;
        }
        coloredCutoutModelCopyLayerRender(this.getParentModel(), this.getParentModel(), TEXTURE,
                poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, partialTick, 1.0F, 1.0F, 1.0F);
    }
}
