package com.Momik.usless_mobs.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renderer for the selectable v2 visual preview. */
public final class FrostStrayV2Renderer extends FrostStrayRenderer {
    public FrostStrayV2Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new FrostStrayV2OverlayLayer<>(this,
                context.bakeLayer(CustomMobModelLayers.FROST_STRAY_V2_OVERLAY)));
    }
}
