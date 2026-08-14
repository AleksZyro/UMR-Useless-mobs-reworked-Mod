package net.mysith.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.mysith.silverfish.CorruptedSilverfishEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CorruptedSilverfishRenderer extends GeoEntityRenderer<CorruptedSilverfishEntity> {
    public CorruptedSilverfishRenderer(EntityRendererProvider.Context context) {
        super(context, new CorruptedSilverfishModel());
        this.shadowRadius = 0.3F;
        addRenderLayer(new CorruptedSilverfishGlowLayer(this));
    }

    public static EntityRenderer<CorruptedSilverfishEntity> createRenderer(EntityRendererProvider.Context context) {
        return new CorruptedSilverfishRenderer(context);
    }
}
