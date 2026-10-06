package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.ability.FrostStrayTimelineClock;
import com.Momik.usless_mobs.ability.FrostStrayTimelineState;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.network.FrostStrayAbilityPacket;
import com.Momik.usless_mobs.network.FrostStrayAbilityPacketEvent;
import com.Momik.usless_mobs.network.FrostStrayImpactPacket;
import com.Momik.usless_mobs.network.FrostStrayImpactPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only cue player. It advances from game time, never from render calls, and makes
 * START/RELEASE/CANCEL idempotent by instance UUID.
 */
@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrostStrayAbilityClient {
    private static final FrostStrayTimelineState STATE = new FrostStrayTimelineState();
    private static final FrostStrayTimelineClock CLOCK = new FrostStrayTimelineClock();
    private static ClientLevel trackedLevel;

    private FrostStrayAbilityClient() {
    }

    public static void handleTimeline(FrostStrayAbilityPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        prepareLevel(level);
        CLOCK.observe(packet.serverGameTime(), level.getGameTime());
        switch (packet.type()) {
            case START -> start(packet, level);
            case RELEASE -> release(packet, level);
            case CANCEL -> cancel(packet);
        }
    }

    public static void handleImpact(FrostStrayImpactPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        prepareLevel(level);
        if (!STATE.recordImpact(packet.impactId(), packet.serverGameTime())) {
            return;
        }
        CLOCK.observe(packet.serverGameTime(), level.getGameTime());
        FrostStrayVfxProfiles.Profile profile = FrostStrayVfxProfiles.impact();
        Vec3 position = new Vec3(packet.x(), packet.y(), packet.z());
        emit(level, position, profile, true, Vec3.ZERO);
        level.playLocalSound(position.x, position.y, position.z,
                com.Momik.usless_mobs.registry.ModSounds.FROST_STRAY_IMPACT.get(),
                net.minecraft.sounds.SoundSource.HOSTILE, 0.78F, 0.86F, false);
    }

    @SubscribeEvent
    public static void onTimelinePacket(FrostStrayAbilityPacketEvent event) {
        handleTimeline(event.packet());
    }

    @SubscribeEvent
    public static void onImpactPacket(FrostStrayImpactPacketEvent event) {
        handleImpact(event.packet());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            STATE.clear();
            trackedLevel = null;
            CLOCK.reset();
            return;
        }
        prepareLevel(level);
        long serverGameTime = CLOCK.serverTime(level.getGameTime());
        STATE.prune(serverGameTime, 20L * 60L);
        for (FrostStrayTimelineState.ActiveTimeline active : STATE.activeTimelines()) {
            tickActive(level, active, serverGameTime);
        }
    }

    private static void start(FrostStrayAbilityPacket packet, ClientLevel level) {
        long elapsed = CLOCK.elapsed(packet.startGameTime(), level.getGameTime());
        FrostStrayTimelineState.StartResult result = STATE.start(packet.entityId(), packet.instanceId(),
                packet.startGameTime(), packet.durationTicks(), CLOCK.serverTime(level.getGameTime()));
        if (result != FrostStrayTimelineState.StartResult.STARTED) {
            return;
        }

        // A late observer joins the current emission, but never receives a stale start sound.
        if (elapsed <= 3L) {
            Entity entity = level.getEntity(packet.entityId());
            if (entity instanceof FrostStrayEntity frostStray) {
                level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        com.Momik.usless_mobs.registry.ModSounds.FROST_STRAY_CHARGE.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.55F, 0.72F, false);
            }
        }
    }

    private static void release(FrostStrayAbilityPacket packet, ClientLevel level) {
        FrostStrayTimelineState.ActiveTimeline active = STATE.activeFor(packet.entityId());
        if (active == null || !active.instanceId().equals(packet.instanceId())) {
            STATE.release(packet.entityId(), packet.instanceId(), packet.serverGameTime());
            return;
        }
        long elapsed = CLOCK.elapsed(packet.startGameTime(), level.getGameTime());
        if (elapsed <= packet.durationTicks() + 3L) {
            Entity entity = level.getEntity(packet.entityId());
            if (entity instanceof FrostStrayEntity frostStray) {
                FrostStrayVfxProfiles.Profile profile = FrostStrayVfxProfiles.release();
                Vec3 releasePoint = FrostStrayVisualAnchors.projectileRelease(frostStray);
                emit(level, releasePoint, profile, true, Vec3.ZERO);
                level.playLocalSound(releasePoint.x, releasePoint.y, releasePoint.z,
                        com.Momik.usless_mobs.registry.ModSounds.FROST_STRAY_RELEASE.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.0F, false);
            }
        }
        STATE.release(packet.entityId(), packet.instanceId(), packet.serverGameTime());
    }

    private static void cancel(FrostStrayAbilityPacket packet) {
        STATE.cancel(packet.entityId(), packet.instanceId(), packet.serverGameTime());
    }

    private static void tickActive(ClientLevel level, FrostStrayTimelineState.ActiveTimeline active,
                                   long serverGameTime) {
        long elapsed = CLOCK.elapsed(active.startGameTime(), level.getGameTime());
        if (elapsed > active.durationTicks() + 4L) {
            STATE.complete(active.entityId(), active.instanceId(), serverGameTime);
            return;
        }
        if (elapsed >= active.durationTicks()) {
            return;
        }
        if (elapsed == active.lastEmissionTick()
                || elapsed % FrostStrayVfxProfiles.charge().emitEveryTicks() != 0L) {
            return;
        }
        active.markEmission(elapsed);
        Entity entity = level.getEntity(active.entityId());
        if (!(entity instanceof FrostStrayEntity frostStray) || !entity.isAlive()) {
            STATE.complete(active.entityId(), active.instanceId(), serverGameTime);
            return;
        }
        Vec3 chargeOrigin = FrostStrayVisualAnchors.chargeOrigin(frostStray);
        Vec3 bowGrip = FrostStrayVisualAnchors.bowGrip(frostStray);
        double chargeProgress = Math.min(1.0D, elapsed / (double) active.durationTicks());
        Vec3 position = chargeOrigin.lerp(bowGrip, 0.18D + chargeProgress * 0.72D);
        Vec3 direction = bowGrip.subtract(position).normalize();
        emit(level, position, FrostStrayVfxProfiles.charge(), false, direction);
    }

    private static void emit(ClientLevel level, Vec3 position, FrostStrayVfxProfiles.Profile profile,
                             boolean oneShot, Vec3 direction) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        double distanceSquared = minecraft.player.distanceToSqr(position);
        int count = profile.countAt(distanceSquared, oneShot);
        ParticleOptions particle = profile.particle();
        for (int index = 0; index < count; index++) {
            double spread = profile.spread();
            level.addParticle(particle, position.x, position.y, position.z,
                    direction.x * profile.directionalSpeed() + (level.random.nextDouble() - 0.5D) * spread,
                    level.random.nextDouble() * profile.verticalSpeed(),
                    direction.z * profile.directionalSpeed() + (level.random.nextDouble() - 0.5D) * spread);
        }
        int accentCount = profile.accentCountAt(distanceSquared, oneShot);
        for (int index = 0; index < accentCount; index++) {
            double spread = profile.spread() * 0.7D;
            level.addParticle(profile.accentParticle(), position.x, position.y, position.z,
                    direction.x * profile.directionalSpeed() * 0.72D
                            + (level.random.nextDouble() - 0.5D) * spread,
                    level.random.nextDouble() * profile.verticalSpeed() * 0.7D,
                    direction.z * profile.directionalSpeed() * 0.72D
                            + (level.random.nextDouble() - 0.5D) * spread);
        }
    }

    /** Uses the same server-time estimate as VFX for the animated bow pose. */
    public static float progressFor(FrostStrayEntity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !entity.isIceVolleyActive()) {
            return 0.0F;
        }
        prepareLevel(level);
        FrostStrayTimelineState.ActiveTimeline active = STATE.activeFor(entity.getId());
        long startGameTime = active == null ? entity.iceVolleyStartTime() : active.startGameTime();
        int durationTicks = active == null ? entity.iceVolleyDurationTicks() : active.durationTicks();
        if (durationTicks <= 0) {
            return 0.0F;
        }
        double elapsed = CLOCK.elapsed(startGameTime, level.getGameTime() + partialTick);
        return (float) Math.max(0.0D, Math.min(1.0D, elapsed / durationTicks));
    }

    private static void prepareLevel(ClientLevel level) {
        if (trackedLevel != level) {
            STATE.clear();
            CLOCK.reset();
            trackedLevel = level;
        }
    }
}
