package net.mysith.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.entity.SoulEndermite;
import net.mysith.MySithMod;
import net.mysith.registry.ModEntities;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class SoulEndermiteSpawnHandler {

    // 1/200'000 Chance bei Enderman-Teleport - extrem rar, schützt gegen Enderman-Farms
    private static final float ENDERMAN_TELEPORT_CHANCE = 1.0F / 200000.0F;

    /** Jede erfolgreiche Gateway-Pearl im End spawnt eine Milbe. */
    private static final int SPAWN_EVERY_N_PEARLS = 1;

    private static final String GATEWAY_THROW_COUNT_KEY = "MysithGatewayPearlCount";
    private static final Map<UUID, Integer> QUEUED_GATEWAY_PEARLS = new HashMap<>();
    private static final Map<UUID, UUID> TRACKED_END_PEARLS = new HashMap<>();
    private static final Set<UUID> VANILLA_PEARL_TELEPORTS = new HashSet<>();
    private static final Set<UUID> QUEUED_GATEWAY_PEARL_IDS = new HashSet<>();

    @SubscribeEvent
    public static void onEnderEntityTeleport(EntityTeleportEvent.EnderEntity event) {
        if (event.getEntity() instanceof EnderMan enderman && enderman.level() instanceof ServerLevel serverLevel) {
            if (enderman.getRandom().nextFloat() < ENDERMAN_TELEPORT_CHANCE) {
                spawnSoulEndermite(serverLevel, enderman.getX(), enderman.getY(), enderman.getZ());
            }
        }
    }

    @SubscribeEvent
    public static void onPearlJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(net.minecraft.world.level.Level.END)) return;
        if (!(event.getEntity() instanceof ThrownEnderpearl pearl)) return;
        if (!(pearl.getOwner() instanceof Player player)) return;

        TRACKED_END_PEARLS.put(pearl.getUUID(), player.getUUID());
        MySithMod.LOGGER.debug("[SoulEndermite] Tracking End pearl {} for {}",
                pearl.getUUID(), player.getName().getString());
    }

    @SubscribeEvent
    public static void onVanillaPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        VANILLA_PEARL_TELEPORTS.add(event.getPearlEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPearlLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(net.minecraft.world.level.Level.END)) return;
        if (!(event.getEntity() instanceof ThrownEnderpearl pearl)) return;

        UUID pearlId = pearl.getUUID();
        UUID playerId = TRACKED_END_PEARLS.remove(pearlId);
        if (playerId == null) {
            Entity owner = pearl.getOwner();
            if (owner instanceof Player player) {
                playerId = player.getUUID();
            }
        }
        if (playerId == null) return;

        if (VANILLA_PEARL_TELEPORTS.remove(pearlId)) {
            MySithMod.LOGGER.debug("[SoulEndermite] End pearl {} used vanilla EnderPearl event; not treating as gateway", pearlId);
            return;
        }

        Player player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) return;

        MySithMod.LOGGER.debug("[SoulEndermite] Inferred gateway pearl {} from discarded End pearl", pearlId);
        queueGatewayPearlTeleport(serverLevel, player, pearlId, "entity-leave-fallback");
    }

    public static void queueGatewayPearlTeleport(ServerLevel serverLevel, Player player) {
        queueGatewayPearlTeleport(serverLevel, player, null, "unknown");
    }

    public static void queueGatewayPearlTeleport(ServerLevel serverLevel, Player player, UUID pearlId, String source) {
        // Vanilla teleportiert Gateway-Pearls in TheEndGatewayBlockEntity direkt per
        // teleportToWithTicket() und discarded die Pearl dabei. Es gibt dafür kein
        // passendes Forge-EnderPearl-Teleport-Event. Der Mixin erkennt die Pearl am
        // Gateway-Eingang und wir spawnen einen Tick später an der echten Exit-Position.
        if (!serverLevel.dimension().equals(net.minecraft.world.level.Level.END)) return;

        if (pearlId != null && !QUEUED_GATEWAY_PEARL_IDS.add(pearlId)) {
            MySithMod.LOGGER.debug("[SoulEndermite] Gateway pearl {} already queued; skipping duplicate from {}",
                    pearlId, source);
            return;
        }

        QUEUED_GATEWAY_PEARLS.merge(player.getUUID(), 1, Integer::sum);
        MySithMod.LOGGER.debug("[SoulEndermite] Queued gateway pearl spawn for {} via {}",
                player.getName().getString(), source);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUED_GATEWAY_PEARLS.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = QUEUED_GATEWAY_PEARLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            iterator.remove();

            Player player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) continue;
            if (!serverLevel.dimension().equals(net.minecraft.world.level.Level.END)) continue;

            for (int i = 0; i < entry.getValue(); i++) {
                onGatewayPearlTeleport(serverLevel, player);
            }
        }

        QUEUED_GATEWAY_PEARL_IDS.clear();
    }

    private static void onGatewayPearlTeleport(ServerLevel serverLevel, Player player) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        int count = data.getInt(GATEWAY_THROW_COUNT_KEY) + 1;
        data.putInt(GATEWAY_THROW_COUNT_KEY, count);

        MySithMod.LOGGER.debug("[SoulEndermite] Gateway pearl teleport #{} for {}", count, player.getName().getString());

        if (count % SPAWN_EVERY_N_PEARLS == 0) {
            spawnSoulEndermite(serverLevel, player.getX(), player.getY(), player.getZ());
        }
    }

    private static void spawnSoulEndermite(ServerLevel level, double x, double y, double z) {
        // Wenn das Pearl-Ziel über Void oder mitten in der Luft ist, snapen wir auf den
        // nächstgelegenen festen Bodenblock — sonst spawnt die Milbe in der Luft und fällt sofort.
        net.minecraft.core.BlockPos ground = findNearSurface(level,
                new net.minecraft.core.BlockPos((int) x, (int) y, (int) z));
        double spawnX = ground != null ? ground.getX() + 0.5 : x;
        double spawnY = ground != null ? ground.getY() + 1 : y;
        double spawnZ = ground != null ? ground.getZ() + 0.5 : z;

        SoulEndermite mite = ModEntities.SOUL_ENDERMITE.get().create(level);
        if (mite == null) return;
        mite.moveTo(spawnX, spawnY, spawnZ, level.getRandom().nextFloat() * 360F, 0F);
        mite.finalizeSpawn(level, level.getCurrentDifficultyAt(mite.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        boolean added = level.addFreshEntity(mite);
        MySithMod.LOGGER.debug("[SoulEndermite] Gateway spawn {} at ({}, {}, {})",
                added ? "added" : "failed", (int) spawnX, (int) spawnY, (int) spawnZ);
    }

    /** Sucht konzentrisch im Radius 0-32 Blocks um die Ziel-Position nach einer festen Surface. */
    private static net.minecraft.core.BlockPos findNearSurface(ServerLevel level, net.minecraft.core.BlockPos around) {
        net.minecraft.core.BlockPos fallback = null;
        double fallbackDistSqr = Double.MAX_VALUE;

        for (int radius = 0; radius <= 32; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dz) < radius) continue;
                    net.minecraft.core.BlockPos sample = level.getHeightmapPos(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                            new net.minecraft.core.BlockPos(around.getX() + dx, 0, around.getZ() + dz));
                    net.minecraft.core.BlockPos ground = sample.below();
                    if (ground.getY() > level.getMinBuildHeight()
                            && level.getBlockState(ground).isCollisionShapeFullBlock(level, ground)) {
                        if (hasStableFooting(level, ground)) return ground;

                        double distSqr = ground.distToCenterSqr(around.getX(), around.getY(), around.getZ());
                        if (distSqr < fallbackDistSqr) {
                            fallback = ground;
                            fallbackDistSqr = distSqr;
                        }
                    }
                }
            }
        }
        return fallback;
    }

    private static boolean hasStableFooting(ServerLevel level, net.minecraft.core.BlockPos ground) {
        return isFullCollision(level, ground.north())
                && isFullCollision(level, ground.south())
                && isFullCollision(level, ground.east())
                && isFullCollision(level, ground.west());
    }

    private static boolean isFullCollision(ServerLevel level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }
}
