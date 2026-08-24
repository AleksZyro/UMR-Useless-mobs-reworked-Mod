package net.mysith.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.mysith.entity.VoidReaperEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VoidReaperRenderer extends GeoEntityRenderer<VoidReaperEntity> {
    public VoidReaperRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidReaperModel());
        this.shadowRadius = 0.65F;
    }

    public static EntityRenderer<VoidReaperEntity> createRenderer(EntityRendererProvider.Context context) {
        return new VoidReaperRenderer(context);
    }
}


