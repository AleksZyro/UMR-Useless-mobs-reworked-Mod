package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.entity.FrostStrayEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Shared visual anchors for Frost Stray cues.
 *
 * <p>The underlying Tripo shell is an unweighted six-region mesh. These anchors are therefore
 * documented entity-local approximations, not a claim that the shell has a hidden weighted hand
 * rig. Keeping the positions in one client-only class makes the charge, release and overlay cues
 * agree while the future weighted replacement remains independent.</p>
 */
final class FrostStrayVisualAnchors {
    private FrostStrayVisualAnchors() {
    }

    static Vec3 chargeOrigin(FrostStrayEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.62D, 0.0D);
    }

    static Vec3 bowGrip(FrostStrayEntity entity) {
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
        return entity.position()
                .add(right.scale(0.27D))
                .add(forward.scale(0.18D))
                .add(0.0D, entity.getBbHeight() * 0.66D, 0.0D);
    }

    static Vec3 projectileRelease(FrostStrayEntity entity) {
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        return bowGrip(entity).add(forward.scale(0.36D)).add(0.0D, 0.04D, 0.0D);
    }
}
