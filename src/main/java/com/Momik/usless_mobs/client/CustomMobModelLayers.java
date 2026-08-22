package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class CustomMobModelLayers {
    public static final ResourceLocation TRANSPARENT_BASE_TEXTURE =
            texture("textures/entity/custom3d/transparent_base.png");
    public static final ModelLayerLocation LIVING_BOSS =
            layer("living_boss_custom_3d");
    public static final ModelLayerLocation FROST_STRAY =
            layer("frost_stray_custom_3d");
    public static final ModelLayerLocation WEB_CAVE_SPIDER =
            layer("web_cave_spider_custom_3d");
    public static final ModelLayerLocation CORAL_DROWNED =
            layer("coral_drowned_custom_3d");
    public static final ModelLayerLocation OCTOPUS =
            layer("octopus_custom_3d");
    public static final ModelLayerLocation WITCH_BOSS =
            layer("witch_boss_custom_3d");
    public static final ModelLayerLocation LIVING_BAT =
            layer("living_bat_custom_3d");
    public static final ModelLayerLocation ROOTED_HUSK =
            layer("rooted_husk_custom_3d");

    public static final ResourceLocation LIVING_BOSS_TEXTURE =
            texture("textures/entity/custom3d/living_boss.png");
    public static final ResourceLocation FROST_STRAY_TEXTURE =
            texture("textures/entity/custom3d/frost_stray.png");
    public static final ResourceLocation WEB_CAVE_SPIDER_TEXTURE =
            texture("textures/entity/custom3d/web_cave_spider.png");
    public static final ResourceLocation CORAL_DROWNED_TEXTURE =
            texture("textures/entity/custom3d/coral_drowned.png");
    public static final ResourceLocation OCTOPUS_TEXTURE =
            texture("textures/entity/custom3d/octopus.png");
    public static final ResourceLocation WITCH_BOSS_TEXTURE =
            texture("textures/entity/custom3d/witch_boss.png");
    public static final ResourceLocation LIVING_BAT_TEXTURE =
            texture("textures/entity/custom3d/living_bat.png");
    public static final ResourceLocation ROOTED_HUSK_TEXTURE =
            texture("textures/entity/custom3d/rooted_husk.png");

    public static final ResourceLocation LIVING_BOSS_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/living_boss.png");
    public static final ResourceLocation WEB_CAVE_SPIDER_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/web_cave_spider.png");
    public static final ResourceLocation OCTOPUS_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/octopus.png");
    public static final ResourceLocation WITCH_BOSS_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/witch_boss.png");
    public static final ResourceLocation LIVING_BAT_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/living_bat.png");
    public static final ResourceLocation ROOTED_HUSK_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/rooted_husk.png");
    public static final ResourceLocation HELPING_ALLAY_EXACT_TEXTURE =
            texture("textures/entity/custom3d/exact/helping_allay.png");

    private CustomMobModelLayers() {}

    private static ModelLayerLocation layer(String path) {
        return new ModelLayerLocation(ResourceLocation.tryBuild(Usless_mobs.MODID, path), "main");
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.tryBuild(Usless_mobs.MODID, path);
    }
}
