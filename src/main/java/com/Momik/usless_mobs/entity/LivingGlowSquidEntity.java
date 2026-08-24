package com.Momik.usless_mobs.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.level.Level;

/** Dedicated UMR glow squid; vanilla glow squids retain their vanilla model. */
public final class LivingGlowSquidEntity extends GlowSquid {
    public LivingGlowSquidEntity(EntityType<? extends GlowSquid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GlowSquid.createAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D);
    }
}
