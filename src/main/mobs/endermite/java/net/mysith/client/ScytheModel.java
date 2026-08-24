package net.mysith.client;

import net.minecraft.resources.ResourceLocation;
import net.mysith.item.BalanceScytheItem;
import net.mysith.item.CelestialScytheItem;
import net.mysith.item.LivingScytheItem;
import net.mysith.item.ScytheItem;
import net.mysith.item.VoidboundScytheItem;
import net.mysith.MySithMod;
import software.bernie.geckolib.model.GeoModel;

public class ScytheModel extends GeoModel<ScytheItem> {

    // Sith = handgemachtes Original (64x64, 798 Farben)
    private static final ResourceLocation SITH_MODEL =
            ResourceLocation.tryBuild(MySithMod.MODID, "geo/sith_scythe.geo.json");
    // Upgrades share the exact Sith family silhouette; texture/theme carries the variant identity.
    private static final ResourceLocation VOIDBOUND_MODEL =
            ResourceLocation.tryBuild(MySithMod.MODID, "geo/voidbound_scythe.geo.json");
    private static final ResourceLocation CELESTIAL_MODEL =
            ResourceLocation.tryBuild(MySithMod.MODID, "geo/celestial_scythe.geo.json");
    private static final ResourceLocation LIVING_MODEL =
            ResourceLocation.tryBuild(MySithMod.MODID, "geo/living_scythe.geo.json");
    private static final ResourceLocation BALANCE_MODEL =
            ResourceLocation.tryBuild(MySithMod.MODID, "geo/balance_scythe.geo.json");

    private static final ResourceLocation SITH_TEX =
            ResourceLocation.tryBuild(MySithMod.MODID, "textures/item/sith_scythe.png");
    private static final ResourceLocation VOIDBOUND_TEX =
            ResourceLocation.tryBuild(MySithMod.MODID, "textures/item/voidbound_scythe.png");
    private static final ResourceLocation CELESTIAL_TEX =
            ResourceLocation.tryBuild(MySithMod.MODID, "textures/item/celestial_scythe.png");
    private static final ResourceLocation LIVING_TEX =
            ResourceLocation.tryBuild(MySithMod.MODID, "textures/item/living_scythe.png");
    private static final ResourceLocation BALANCE_TEX =
            ResourceLocation.tryBuild(MySithMod.MODID, "textures/item/balance_scythe.png");

    private static final ResourceLocation ANIMATION =
            ResourceLocation.tryBuild(MySithMod.MODID, "animations/sith_scythe.animation.json");

    @Override
    public ResourceLocation getModelResource(ScytheItem animatable) {
        if (animatable instanceof VoidboundScytheItem)  return VOIDBOUND_MODEL;
        if (animatable instanceof CelestialScytheItem)  return CELESTIAL_MODEL;
        if (animatable instanceof LivingScytheItem)     return LIVING_MODEL;
        if (animatable instanceof BalanceScytheItem)    return BALANCE_MODEL;
        return SITH_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ScytheItem animatable) {
        if (animatable instanceof VoidboundScytheItem)  return VOIDBOUND_TEX;
        if (animatable instanceof CelestialScytheItem)  return CELESTIAL_TEX;
        if (animatable instanceof LivingScytheItem)     return LIVING_TEX;
        if (animatable instanceof BalanceScytheItem)    return BALANCE_TEX;
        return SITH_TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(ScytheItem animatable) {
        return ANIMATION;
    }
}
