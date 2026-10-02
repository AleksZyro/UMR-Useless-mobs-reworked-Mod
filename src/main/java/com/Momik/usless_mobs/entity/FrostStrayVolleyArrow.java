package com.Momik.usless_mobs.entity;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Arrow variant that reports an impact only after the server has observed a real collision. */
final class FrostStrayVolleyArrow extends Arrow {
    private final UUID abilityInstanceId;
    private boolean impactReported;

    FrostStrayVolleyArrow(Level level, LivingEntity owner, UUID abilityInstanceId) {
        super(level, owner);
        this.abilityInstanceId = abilityInstanceId;
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide && !this.impactReported && this.getOwner() instanceof FrostStrayEntity frostStray) {
            this.impactReported = true;
            frostStray.onVolleyProjectileImpact(this.abilityInstanceId, result.getLocation());
        }
        super.onHit(result);
    }
}
