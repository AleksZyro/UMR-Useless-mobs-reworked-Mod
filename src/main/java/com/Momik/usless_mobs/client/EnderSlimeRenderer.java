package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;

public class EnderSlimeRenderer extends SlimeRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(Usless_mobs.MODID, "textures/entity/ender_schleim.png");

    public EnderSlimeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    public static EntityRenderer<EnderSlimeEntity> createRenderer(EntityRendererProvider.Context context) {
        return (EntityRenderer<EnderSlimeEntity>) (EntityRenderer<?>) new EnderSlimeRenderer(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Slime slime) {
        return TEXTURE;
    }
}
