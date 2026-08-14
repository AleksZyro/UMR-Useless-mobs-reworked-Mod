package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class RootedHuskRenderer extends HuskRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/living/husk.png");

    public RootedHuskRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.ROOTED_HUSK),
                CustomMob3DModel.Variant.ROOTED_HUSK, CustomMobModelLayers.ROOTED_HUSK_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }
}
