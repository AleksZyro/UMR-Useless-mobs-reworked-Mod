package com.Momik.usless_mobs.client;

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
    private static long serverTimeOffset;

    private FrostStrayAbilityClient() {
    }

    public static void handleTimeline(FrostStrayAbilityPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        serverTimeOffset = packet.serverGameTime() - level.getGameTime();
        switch (packet.type()) {
            case START -> start(packet, level);
            case RELEASE -> release(packet, level);
            case CANCEL -> cancel(packet);
        }
    }

    public static void handleImpact(FrostStrayImpactPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || IMPACTS.putIfAbsent(packet.impactId(), packet.serverGameTime()) != null) {
            return;
        }
        serverTimeOffset = packet.serverGameTime() - level.getGameTime();
        FrostStrayVfxProfiles.Profile profile = FrostStrayVfxProfiles.impact();
        emit(level, new Vec3(packet.x(), packet.y(), packet.z()), profile, true);
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
            return;
        }
        long serverGameTime = level.getGameTime() + serverTimeOffset;
        FINISHED.entrySet().removeIf(entry -> entry.getValue() < serverGameTime - 20L * 60L);
        IMPACTS.entrySet().removeIf(entry -> entry.getValue() < serverGameTime - 20L * 60L);
        ACTIVE.entrySet().removeIf(entry -> tickActive(level, entry.getValue(), serverGameTime));
    }

    private static void start(FrostStrayAbilityPacket packet, ClientLevel level) {
        if (FINISHED.containsKey(packet.instanceId())) {
            return;
        }
        long elapsed = Math.max(0L, level.getGameTime() + serverTimeOffset - packet.startGameTime());
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
            if (entity != null) {
                level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        com.Momik.usless_mobs.registry.ModSounds.FROST_STRAY_VOLLEY.get(),
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
        long elapsed = Math.max(0L, level.getGameTime() + serverTimeOffset - packet.startGameTime());
        if (elapsed <= packet.durationTicks() + 3L) {
            Entity entity = level.getEntity(packet.entityId());
            if (entity != null) {
                FrostStrayVfxProfiles.Profile profile = FrostStrayVfxProfiles.release();
                emit(level, entity.position().add(0.0D, entity.getEyeHeight(), 0.0D), profile, true);
                level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        com.Momik.usless_mobs.registry.ModSounds.FROST_STRAY_VOLLEY.get(),
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
        long elapsed = Math.max(0L, serverGameTime - active.startGameTime);
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
        if (entity == null || !entity.isAlive()) {
            finish(active.instanceId, serverGameTime);
            return true;
        }
        emit(level, entity.position().add(0.0D, entity.getEyeHeight() * 0.8D, 0.0D),
                FrostStrayVfxProfiles.charge(), false);
        return false;
    }

    private static void emit(ClientLevel level, Vec3 position, FrostStrayVfxProfiles.Profile profile,
                             boolean oneShot) {
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
                    (level.random.nextDouble() - 0.5D) * spread,
                    level.random.nextDouble() * profile.verticalSpeed(),
                    (level.random.nextDouble() - 0.5D) * spread);
        }
    }

    private static void finish(UUID instanceId, long serverGameTime) {
        FINISHED.put(instanceId, serverGameTime);
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
