package com.Momik.usless_mobs.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;

/** Dedicated UMR squid; vanilla squids keep their vanilla model and behaviour. */
public final class LivingSquidEntity extends Squid {
    public LivingSquidEntity(EntityType<? extends Squid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Squid.createAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D);
    }
}
