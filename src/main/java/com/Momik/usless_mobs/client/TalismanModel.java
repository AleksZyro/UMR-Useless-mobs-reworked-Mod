package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.item.PathTalismanItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TalismanModel extends GeoModel<PathTalismanItem> {

    private static final ResourceLocation VOID_MODEL =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "geo/void_talisman.geo.json");
    private static final ResourceLocation CELESTIAL_MODEL =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "geo/celestial_talisman.geo.json");
    private static final ResourceLocation LIVING_MODEL =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "geo/living_talisman.geo.json");

    private static final ResourceLocation VOID_TEX =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/item/void_talisman_geo.png");
    private static final ResourceLocation CELESTIAL_TEX =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/item/celestial_talisman_geo.png");
    private static final ResourceLocation LIVING_TEX =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/item/living_talisman_geo.png");

    private static final ResourceLocation ANIMATION =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "animations/talisman.animation.json");

    @Override
    public ResourceLocation getModelResource(PathTalismanItem animatable) {
        return switch (animatable.getPath()) {
            case CELESTIAL -> CELESTIAL_MODEL;
            case LIVING    -> LIVING_MODEL;
            default        -> VOID_MODEL;
        };
    }

    @Override
    public ResourceLocation getTextureResource(PathTalismanItem animatable) {
        return switch (animatable.getPath()) {
            case CELESTIAL -> CELESTIAL_TEX;
            case LIVING    -> LIVING_TEX;
            default        -> VOID_TEX;
        };
    }

    @Override
    public ResourceLocation getAnimationResource(PathTalismanItem animatable) {
        return ANIMATION;
    }
}
