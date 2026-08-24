package net.mysith.client;

import net.mysith.silverfish.CorruptedSilverfishEntity;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class CorruptedSilverfishGlowLayer extends AutoGlowingGeoLayer<CorruptedSilverfishEntity> {
    public CorruptedSilverfishGlowLayer(GeoRenderer<CorruptedSilverfishEntity> renderer) {
        super(renderer);
    }
}
