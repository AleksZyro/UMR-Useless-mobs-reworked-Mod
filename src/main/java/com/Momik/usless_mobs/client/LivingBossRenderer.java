package com.Momik.usless_mobs.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RavagerRenderer;

public class LivingBossRenderer extends RavagerRenderer {
    public LivingBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LivingBossModel(context.bakeLayer(ModelLayers.RAVAGER));
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.LIVING_BOSS),
                CustomMob3DModel.Variant.LIVING_BOSS, CustomMobModelLayers.LIVING_BOSS_TEXTURE));
    }
}
