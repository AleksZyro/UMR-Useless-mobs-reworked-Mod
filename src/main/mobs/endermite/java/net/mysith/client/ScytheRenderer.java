package net.mysith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.mysith.item.ScytheItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ScytheRenderer extends GeoItemRenderer<ScytheItem> {
    private ItemDisplayContext currentContext = ItemDisplayContext.NONE;

    public ScytheRenderer() {
        super(new ScytheModel());
        // Sonder-Shader bleibt aus: die Voidbound-Klinge soll als klare graue Metallklinge
        // sichtbar sein, nicht als riesige Shader-Fläche in der Hand.
        // Emissiver Theme-Glow nur für celestial/living/balance (eigener Layer überspringt
        // sith/void -> kein Glowmask-Load für die, kein Crash).
        addRenderLayer(new ScytheGlowLayer(this));
    }

    /** True im Inventar-GUI -- dort soll der Glow nicht über das Icon rendern. */
    public boolean isGuiContext() {
        return currentContext == ItemDisplayContext.GUI;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        this.currentContext = displayContext;
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    // Vor dem Hauptpass: blade-Bone verstecken, wenn sie stattdessen per Portal-Layer kommt.
    // Wichtig, da die Scythe-Familie dieselbe Sith-Silhouette nutzt -> jedes Mal
    // explizit setzen.
    @Override
    public void preRender(PoseStack poseStack, ScytheItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        GeoBone blade = model.getBone("blade").orElse(null);
        if (blade != null) {
            blade.setHidden(false);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
