package com.Momik.usless_mobs.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.Level;

/** Dedicated UMR axolotl; vanilla axolotls retain their vanilla model. */
public final class LivingAxolotlEntity extends Axolotl {
    public LivingAxolotlEntity(EntityType<? extends Axolotl> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Axolotl.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D);
    }
}
