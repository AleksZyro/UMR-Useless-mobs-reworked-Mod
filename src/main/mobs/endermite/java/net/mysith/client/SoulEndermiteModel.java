package net.mysith.client;

import net.minecraft.resources.ResourceLocation;
import net.mysith.entity.SoulEndermite;
import net.mysith.MySithMod;
import software.bernie.geckolib.model.GeoModel;

public class SoulEndermiteModel extends GeoModel<SoulEndermite> {

    private static final ResourceLocation MODEL =
            ResourceLocation.tryBuild(MySithMod.MODID, "geo/soul_endermite.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(MySithMod.MODID, "textures/entity/soul_endermite.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.tryBuild(MySithMod.MODID, "animations/soul_endermite.animation.json");

    @Override
    public ResourceLocation getModelResource(SoulEndermite animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SoulEndermite animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SoulEndermite animatable) {
        return ANIMATION;
    }
}
