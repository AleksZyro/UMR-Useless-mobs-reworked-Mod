package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CelestialSlimeModel extends GeoModel<CelestialSlimeEntity> {
    private static final ResourceLocation GEO = ResourceLocation.tryBuild(Usless_mobs.MODID, "geo/celestial_slime.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/celestial_slime.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.tryBuild(Usless_mobs.MODID, "animations/celestial_slime.animation.json");

    @Override
    public ResourceLocation getModelResource(CelestialSlimeEntity entity) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(CelestialSlimeEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CelestialSlimeEntity entity) {
        return ANIMATION;
    }
}
