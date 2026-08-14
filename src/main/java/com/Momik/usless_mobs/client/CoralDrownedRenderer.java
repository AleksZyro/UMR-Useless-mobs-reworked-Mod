package com.Momik.usless_mobs.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CoralDrownedRenderer extends DrownedRenderer {
    public CoralDrownedRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CoralDrownedModel<>(context.bakeLayer(ModelLayers.DROWNED));
        this.addLayer(new CoralOverlayLayer(this));
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.CORAL_DROWNED),
                CustomMob3DModel.Variant.CORAL_DROWNED, CustomMobModelLayers.CORAL_DROWNED_TEXTURE));
    }
}
