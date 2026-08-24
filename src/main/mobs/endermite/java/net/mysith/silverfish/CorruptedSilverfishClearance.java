package net.mysith.silverfish;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class CorruptedSilverfishClearance {
    private static final int STUCK_TICKS_BEFORE_ESCAPE = 40;
    private static final int MAX_SAFE_POSITION_AGE = 20 * 12;
    private static final double MAX_ESCAPE_DISTANCE_SQR = 4.0D * 4.0D;
    private static final double MIN_MOVEMENT_SQR = 0.02D * 0.02D;

    private final CorruptedSilverfishEntity parent;
    private Vec3 lastSafePosition;
    private int lastSafePositionTick = Integer.MIN_VALUE;
    private Vec3 previousPosition;
    private int stuckTicks;

    CorruptedSilverfishClearance(CorruptedSilverfishEntity parent) {
        this.parent = parent;
    }

    void tick() {
        if (this.parent.level().isClientSide || !this.parent.isAlive()) {
            return;
        }

        AABB bounds = fullBodyBounds();
        boolean clear = hasFullBodyClearance(bounds);
        boolean supported = hasSafeBodySupport();
        Vec3 currentPosition = this.parent.position();
        double movementSqr = this.previousPosition == null
                ? Double.MAX_VALUE
                : horizontalDistanceSqr(currentPosition, this.previousPosition);

        if (clear && supported && this.parent.onGround()) {
            this.lastSafePosition = currentPosition;
            this.lastSafePositionTick = this.parent.tickCount;
            this.stuckTicks = 0;
        } else if (!clear || !supported) {
            this.parent.getNavigation().stop();
            Vec3 velocity = this.parent.getDeltaMovement();
            this.parent.setDeltaMovement(0.0D, Math.min(velocity.y, 0.0D), 0.0D);
            if (this.parent.horizontalCollision || movementSqr < MIN_MOVEMENT_SQR || !supported) {
                this.stuckTicks++;
            }
        } else {
            this.stuckTicks = Math.max(0, this.stuckTicks - 1);
        }

        if (this.stuckTicks >= STUCK_TICKS_BEFORE_ESCAPE) {
            tryEscapeToLastSafePosition();
        }
        this.previousPosition = this.parent.position();
    }

    AABB fullBodyBounds() {
        AABB bounds = this.parent.getBoundingBox();
        for (CorruptedSilverfishPart part : this.parent.damageParts()) {
            bounds = bounds.minmax(part.getBoundingBox());
        }
        return bounds.deflate(0.03D);
    }

    boolean hasFullBodyClearance(AABB bounds) {
        return this.parent.level().noCollision(this.parent, bounds);
    }

    boolean hasSafeBodySupport() {
        if (!this.parent.onGround()) {
            return true;
        }
        for (CorruptedSilverfishPart part : this.parent.damageParts()) {
            BlockPos floor = BlockPos.containing(part.getX(), this.parent.getY() - 0.08D, part.getZ());
            if (this.parent.level().getBlockState(floor).getCollisionShape(this.parent.level(), floor).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void tryEscapeToLastSafePosition() {
        this.stuckTicks = 0;
        if (this.lastSafePosition == null
                || this.parent.tickCount - this.lastSafePositionTick > MAX_SAFE_POSITION_AGE
                || this.parent.position().distanceToSqr(this.lastSafePosition) > MAX_ESCAPE_DISTANCE_SQR) {
            return;
        }

        AABB destinationBounds = this.parent.getBoundingBox().move(this.lastSafePosition.subtract(this.parent.position()));
        if (!this.parent.level().noCollision(this.parent, destinationBounds)) {
            return;
        }

        Vec3 departure = this.parent.position();
        this.parent.teleportTo(this.lastSafePosition.x, this.lastSafePosition.y, this.lastSafePosition.z);
        this.parent.setDeltaMovement(Vec3.ZERO);
        this.parent.onCorruptionEscape(departure, this.lastSafePosition);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }
}
