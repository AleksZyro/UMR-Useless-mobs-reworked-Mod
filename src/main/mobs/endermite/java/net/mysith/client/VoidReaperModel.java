package net.mysith.client;

import net.minecraft.resources.ResourceLocation;
import net.mysith.entity.VoidReaperEntity;
import net.mysith.MySithMod;
import software.bernie.geckolib.model.GeoModel;

public class VoidReaperModel extends GeoModel<VoidReaperEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.tryBuild(MySithMod.MODID, "geo/void_reaper.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(MySithMod.MODID, "textures/entity/void_reaper.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.tryBuild(MySithMod.MODID, "animations/void_reaper.animation.json");

    @Override
    public ResourceLocation getModelResource(VoidReaperEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VoidReaperEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VoidReaperEntity entity) {
        return ANIMATION;
    }
}
