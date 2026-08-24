package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.WebCaveSpiderEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.CaveSpider;

public class WebCaveSpiderRenderer extends CaveSpiderRenderer {
    public WebCaveSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WebCaveSpiderModel<>(context.bakeLayer(ModelLayers.CAVE_SPIDER));
        this.layers.clear();
        this.shadowRadius = 0.65F;
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.WEB_CAVE_SPIDER,
                CustomMobModelLayers.WEB_CAVE_SPIDER_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(CaveSpider entity) {
        if (entity instanceof WebCaveSpiderEntity) {
            return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
