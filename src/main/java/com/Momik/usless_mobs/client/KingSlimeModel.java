package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KingSlimeModel extends GeoModel<KingSlimeEntity> {

    private static final ResourceLocation GEO = ResourceLocation.tryBuild(Usless_mobs.MODID, "geo/king_slime.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/king_slime_geo.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.tryBuild(Usless_mobs.MODID, "animations/king_slime.animation.json");

    @Override
    public ResourceLocation getModelResource(KingSlimeEntity entity) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(KingSlimeEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(KingSlimeEntity entity) {
        return ANIMATION;
    }
}
