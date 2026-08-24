package com.Momik.usless_mobs.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.level.Level;

/** Dedicated UMR jungle hunter; vanilla ocelots retain their vanilla model. */
public final class LivingOcelotEntity extends Ocelot {
    public LivingOcelotEntity(EntityType<? extends Ocelot> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Ocelot.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }
}
