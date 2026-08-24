package net.mysith.client;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.mysith.silverfish.CorruptedSilverfishEntity;
import software.bernie.geckolib.model.GeoModel;

public class CorruptedSilverfishModel extends GeoModel<CorruptedSilverfishEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.tryBuild(Usless_mobs.MODID, "geo/corrupted_silverfish.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/corrupted_silverfish.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.tryBuild(Usless_mobs.MODID, "animations/corrupted_silverfish.animation.json");

    @Override
    public ResourceLocation getModelResource(CorruptedSilverfishEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CorruptedSilverfishEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CorruptedSilverfishEntity entity) {
        return ANIMATION;
    }
}
