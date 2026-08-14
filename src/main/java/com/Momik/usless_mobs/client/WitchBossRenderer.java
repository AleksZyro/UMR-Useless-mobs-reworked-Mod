package com.Momik.usless_mobs.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitchRenderer;

public class WitchBossRenderer extends WitchRenderer {
    public WitchBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WitchBossModel<>(context.bakeLayer(ModelLayers.WITCH));
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.WITCH_BOSS),
                CustomMob3DModel.Variant.WITCH_BOSS, CustomMobModelLayers.WITCH_BOSS_TEXTURE));
    }
}
