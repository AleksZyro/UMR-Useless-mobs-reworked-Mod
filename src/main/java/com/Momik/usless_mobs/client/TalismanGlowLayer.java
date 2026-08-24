package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.Momik.usless_mobs.item.PathTalismanItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Emissiver Gem-Glow für alle drei Talismane. Nutzt die *_glowmask.png neben der
 * Basistextur (nur Gem/Auge/Adern gesetzt). Im GUI-Kontext übersprungen, damit der
 * Glow nicht über das Inventar-Icon rendert (gleiches Muster wie ScytheGlowLayer).
 */
public class TalismanGlowLayer extends AutoGlowingGeoLayer<PathTalismanItem> {
    public TalismanGlowLayer(GeoRenderer<PathTalismanItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, PathTalismanItem animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (getRenderer() instanceof TalismanRenderer tr && tr.isGuiContext()) {
            return;
        }
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }
}
