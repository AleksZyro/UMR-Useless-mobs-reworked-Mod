package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.Momik.usless_mobs.item.PathTalismanItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TalismanRenderer extends GeoItemRenderer<PathTalismanItem> {
    private ItemDisplayContext currentContext = ItemDisplayContext.NONE;

    public TalismanRenderer() {
        super(new TalismanModel());
        addRenderLayer(new TalismanGlowLayer(this));
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
}
