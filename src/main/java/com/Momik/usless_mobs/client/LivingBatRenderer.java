package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;

public class LivingBatRenderer extends BatRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/living/bat.png");

    public LivingBatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new CustomMob3DLayer<>(this, context.bakeLayer(CustomMobModelLayers.LIVING_BAT),
                CustomMob3DModel.Variant.LIVING_BAT, CustomMobModelLayers.LIVING_BAT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Bat entity) {
        return TEXTURE;
    }
}
