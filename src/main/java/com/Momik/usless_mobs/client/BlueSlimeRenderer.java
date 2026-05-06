package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;

public class BlueSlimeRenderer extends SlimeRenderer {

    private static final ResourceLocation BLUE_TEXTURE = ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/blauer_schleim.png");
    private static final ResourceLocation GOLD_TEXTURE = ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/goldener_schleim.png");

    public BlueSlimeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    public static EntityRenderer<BlueSlimeEntity> createRenderer(EntityRendererProvider.Context context) {
        return (EntityRenderer<BlueSlimeEntity>) (EntityRenderer<?>) new BlueSlimeRenderer(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Slime slime) {
        if (slime instanceof BlueSlimeEntity blueSlime && blueSlime.isGolden()) {
            return GOLD_TEXTURE;
        }

        return BLUE_TEXTURE;
    }
}
