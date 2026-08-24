package net.mysith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.mysith.item.BalanceScytheItem;
import net.mysith.item.CelestialScytheItem;
import net.mysith.item.LivingScytheItem;
import net.mysith.item.ScytheItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Emissiver Theme-Glow nur für celestial/living/balance. Sith (Original 1:1, keine Glowmask)
 * und Voidbound (graue Metallklinge, keine Glowmask) werden übersprungen -- damit GeckoLib für sie keine
 * (leere) Glowmask laden muss (das würde mit "must have at least one pixel" crashen).
 */
public class ScytheGlowLayer extends AutoGlowingGeoLayer<ScytheItem> {
    public ScytheGlowLayer(GeoRenderer<ScytheItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, ScytheItem animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (getRenderer() instanceof ScytheRenderer sr && sr.isGuiContext()) {
            return; // im Inventar-Icon kein Glow-Overlay
        }
        if (animatable instanceof CelestialScytheItem
                || animatable instanceof LivingScytheItem
                || animatable instanceof BalanceScytheItem) {
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
        }
    }
}
