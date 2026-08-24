package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class CoralDrownedRenderer extends DrownedRenderer {
    public CoralDrownedRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CoralDrownedModel<>(context.bakeLayer(ModelLayers.DROWNED));
        this.layers.clear();
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new ExactMobMeshLayer<>(this, context.getResourceManager(),
                CustomMob3DModel.Variant.CORAL_DROWNED, CustomMobModelLayers.CORAL_DROWNED_EXACT_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        if (entity instanceof CoralDrownedEntity) {
            return CustomMobModelLayers.TRANSPARENT_BASE_TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
