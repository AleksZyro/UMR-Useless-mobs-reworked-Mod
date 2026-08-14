package net.mysith.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mysith.silverfish.CorruptedSilverfishEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CorruptedSilverfishRenderer extends GeoEntityRenderer<CorruptedSilverfishEntity> {
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            "usless_mobs", "textures/entity/corrupted_silverfish_glowmask.png");

    public CorruptedSilverfishRenderer(EntityRendererProvider.Context context) {
        super(context, new CorruptedSilverfishModel());
        this.shadowRadius = 0.3F;
        // The v3 glowmask is a protected candidate; register only when an active pack supplies it.
        if (context.getResourceManager().getResource(GLOWMASK).isPresent()) {
            addRenderLayer(new CorruptedSilverfishGlowLayer(this));
        }
    }

    public static EntityRenderer<CorruptedSilverfishEntity> createRenderer(EntityRendererProvider.Context context) {
        return new CorruptedSilverfishRenderer(context);
    }
}
