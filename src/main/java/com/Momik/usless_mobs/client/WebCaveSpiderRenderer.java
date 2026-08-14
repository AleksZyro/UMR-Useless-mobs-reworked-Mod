package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.WebCaveSpiderEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.CaveSpider;

public class WebCaveSpiderRenderer extends CaveSpiderRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/living/cave_spider.png");

    public WebCaveSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WebCaveSpiderModel<>(context.bakeLayer(ModelLayers.CAVE_SPIDER));
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.WEB_CAVE_SPIDER),
                CustomMob3DModel.Variant.WEB_CAVE_SPIDER, CustomMobModelLayers.WEB_CAVE_SPIDER_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(CaveSpider entity) {
        if (entity instanceof WebCaveSpiderEntity) {
            return TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
