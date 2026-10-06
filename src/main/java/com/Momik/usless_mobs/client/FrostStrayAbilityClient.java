package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.ability.FrostStrayTimelineClock;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.network.FrostStrayAbilityPacket;
import com.Momik.usless_mobs.network.FrostStrayAbilityPacketEvent;
import com.Momik.usless_mobs.network.FrostStrayImpactPacket;
import com.Momik.usless_mobs.network.FrostStrayImpactPacketEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private static final Map<Integer, ActiveTimeline> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> FINISHED = new HashMap<>();
    private static final Map<UUID, Long> IMPACTS = new HashMap<>();
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
        if (IMPACTS.putIfAbsent(packet.impactId(), packet.serverGameTime()) != null) {
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
            ACTIVE.clear();
            FINISHED.clear();
            IMPACTS.clear();
            trackedLevel = null;
            CLOCK.reset();
            return;
        }
        prepareLevel(level);
        long serverGameTime = CLOCK.serverTime(level.getGameTime());
        FINISHED.entrySet().removeIf(entry -> entry.getValue() < serverGameTime - 20L * 60L);
        IMPACTS.entrySet().removeIf(entry -> entry.getValue() < serverGameTime - 20L * 60L);
        ACTIVE.entrySet().removeIf(entry -> tickActive(level, entry.getValue(), serverGameTime));
    }

    private static void start(FrostStrayAbilityPacket packet, ClientLevel level) {
        if (FINISHED.containsKey(packet.instanceId())) {
            return;
        }
        long elapsed = CLOCK.elapsed(packet.startGameTime(), level.getGameTime());
        if (elapsed >= packet.durationTicks()) {
            finish(packet.instanceId(), packet.serverGameTime());
            return;
        }
        ActiveTimeline existing = ACTIVE.get(packet.entityId());
        if (existing != null) {
            if (existing.instanceId.equals(packet.instanceId()) || existing.startGameTime > packet.startGameTime()) {
                return;
            }
            finish(existing.instanceId, packet.serverGameTime());
        }
        ACTIVE.put(packet.entityId(), new ActiveTimeline(packet.entityId(), packet.instanceId(),
                packet.startGameTime(), packet.durationTicks()));

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
        ActiveTimeline active = ACTIVE.get(packet.entityId());
        if (active == null || !active.instanceId.equals(packet.instanceId())) {
            finish(packet.instanceId(), packet.serverGameTime());
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
        ACTIVE.remove(packet.entityId());
        finish(packet.instanceId(), packet.serverGameTime());
    }

    private static void cancel(FrostStrayAbilityPacket packet) {
        ActiveTimeline active = ACTIVE.get(packet.entityId());
        if (active != null && active.instanceId.equals(packet.instanceId())) {
            ACTIVE.remove(packet.entityId());
        }
        finish(packet.instanceId(), packet.serverGameTime());
    }

    private static boolean tickActive(ClientLevel level, ActiveTimeline active, long serverGameTime) {
        long elapsed = CLOCK.elapsed(active.startGameTime, level.getGameTime());
        if (elapsed > active.durationTicks + 4L) {
            finish(active.instanceId, serverGameTime);
            return true;
        }
        if (elapsed >= active.durationTicks) {
            return false;
        }
        if (elapsed == active.lastEmissionTick || elapsed % FrostStrayVfxProfiles.charge().emitEveryTicks() != 0L) {
            return false;
        }
        active.lastEmissionTick = elapsed;
        Entity entity = level.getEntity(active.entityId);
        if (!(entity instanceof FrostStrayEntity frostStray) || !entity.isAlive()) {
            finish(active.instanceId, serverGameTime);
            return true;
        }
        Vec3 chargeOrigin = FrostStrayVisualAnchors.chargeOrigin(frostStray);
        Vec3 bowGrip = FrostStrayVisualAnchors.bowGrip(frostStray);
        double chargeProgress = Math.min(1.0D, elapsed / (double) active.durationTicks);
        Vec3 position = chargeOrigin.lerp(bowGrip, 0.18D + chargeProgress * 0.72D);
        Vec3 direction = bowGrip.subtract(position).normalize();
        emit(level, position, FrostStrayVfxProfiles.charge(), false, direction);
        return false;
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

    private static void finish(UUID instanceId, long serverGameTime) {
        FINISHED.put(instanceId, serverGameTime);
    }

    /** Uses the same server-time estimate as VFX for the animated bow pose. */
    public static float progressFor(FrostStrayEntity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !entity.isIceVolleyActive()) {
            return 0.0F;
        }
        prepareLevel(level);
        ActiveTimeline active = ACTIVE.get(entity.getId());
        long startGameTime = active == null ? entity.iceVolleyStartTime() : active.startGameTime;
        int durationTicks = active == null ? entity.iceVolleyDurationTicks() : active.durationTicks;
        if (durationTicks <= 0) {
            return 0.0F;
        }
        double elapsed = CLOCK.elapsed(startGameTime, level.getGameTime() + partialTick);
        return (float) Math.max(0.0D, Math.min(1.0D, elapsed / durationTicks));
    }

    private static void prepareLevel(ClientLevel level) {
        if (trackedLevel != level) {
            ACTIVE.clear();
            FINISHED.clear();
            IMPACTS.clear();
            CLOCK.reset();
            trackedLevel = level;
        }
    }

    private static final class ActiveTimeline {
        private final int entityId;
        private final UUID instanceId;
        private final long startGameTime;
        private final int durationTicks;
        private long lastEmissionTick = Long.MIN_VALUE;

        private ActiveTimeline(int entityId, UUID instanceId, long startGameTime, int durationTicks) {
            this.entityId = entityId;
            this.instanceId = instanceId;
            this.startGameTime = startGameTime;
            this.durationTicks = durationTicks;
        }
    }
}
