package com.Momik.usless_mobs.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Witch;

public class WitchBossRenderer extends WitchRenderer {
    public WitchBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WitchBossModel<>(context.bakeLayer(ModelLayers.WITCH));
        this.layers.clear();
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.WITCH_BOSS, CustomMobModelLayers.WITCH_BOSS_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Witch entity) {
        return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
    }
}
