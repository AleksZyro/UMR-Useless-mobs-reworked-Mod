package com.Momik.usless_mobs.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ravager;

public class LivingBossRenderer extends RavagerRenderer {
    public LivingBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LivingBossModel(context.bakeLayer(ModelLayers.RAVAGER));
        this.layers.clear();
        this.shadowRadius = 1.45F;
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.LIVING_BOSS, CustomMobModelLayers.LIVING_BOSS_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Ravager entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
