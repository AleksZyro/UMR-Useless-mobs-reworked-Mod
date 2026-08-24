package net.mysith.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.mysith.entity.SoulEndermite;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SoulEndermiteRenderer extends GeoEntityRenderer<SoulEndermite> {
    public SoulEndermiteRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulEndermiteModel());
    }
}
