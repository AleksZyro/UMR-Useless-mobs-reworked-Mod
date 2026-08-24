package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class LivingRaidHandler {
    private static final String RAID_OWNER_KEY = "UslessMobsLivingRaidOwner";
    private static final long RAID_TIMEOUT_TICKS = 20L * 60L * 8L;
    private static final long RAID_CLEANUP_INTERVAL_TICKS = 40L;
    private static final Map<UUID, Integer> ACTIVE_RAIDS = new HashMap<>();
    private static final Map<UUID, Long> RAID_TIMEOUTS = new HashMap<>();
    private static long lastCleanupTick;

    private LivingRaidHandler() {
    }

    public static boolean startRaid(ServerLevel level, ServerPlayer player, BlockPos center) {
        UUID owner = player.getUUID();
        if (ACTIVE_RAIDS.containsKey(owner)) {
            return false;
        }

        int requestedCount = level.getDifficulty() == Difficulty.HARD ? 8 : 6;
        int spawnedCount = 0;
        for (int i = 0; i < requestedCount; i++) {
            FrostStrayEntity stray = com.Momik.usless_mobs.registry.ModEntities.FROST_STRAY.get().create(level);
            if (stray == null) {
                continue;
            }
            double angle = (i / (double) requestedCount) * Math.PI * 2.0D;
            double radius = 7.0D + level.random.nextDouble() * 4.0D;
            BlockPos spawnPos = center.offset((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
            spawnPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos);
            stray.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            stray.setTarget(player);
            stray.setPersistenceRequired();
            stray.getPersistentData().putUUID(RAID_OWNER_KEY, owner);
            stray.setCustomName(Component.translatable("entity.usless_mobs.frost_stray.raid"));
            if (level.addFreshEntity(stray)) {
                spawnedCount++;
            }
        }

        if (spawnedCount == 0) {
            return false;
        }

        ACTIVE_RAIDS.put(owner, spawnedCount);
        RAID_TIMEOUTS.put(owner, level.getGameTime() + RAID_TIMEOUT_TICKS);
        player.displayClientMessage(Component.translatable("item.usless_mobs.living_crystal.raid_started")
                .withStyle(ChatFormatting.DARK_AQUA), false);
        level.playSound(null, center, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.2F, 1.25F);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now - lastCleanupTick < RAID_CLEANUP_INTERVAL_TICKS) {
            return;
        }
        lastCleanupTick = now;

        Iterator<Map.Entry<UUID, Long>> iterator = RAID_TIMEOUTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() > now) {
                continue;
            }

            UUID owner = entry.getKey();
            iterator.remove();
            ACTIVE_RAIDS.remove(owner);
            discardRaidMobs(server, owner);

            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player != null) {
                player.displayClientMessage(Component.translatable("item.usless_mobs.living_crystal.raid_faded")
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();
        if (dead instanceof ServerPlayer player) {
            if (ACTIVE_RAIDS.remove(player.getUUID()) != null) {
                RAID_TIMEOUTS.remove(player.getUUID());
                discardRaidMobs(player.server, player.getUUID());
                player.displayClientMessage(Component.translatable("item.usless_mobs.living_crystal.raid_failed")
                        .withStyle(ChatFormatting.RED), false);
            }
            return;
        }

        CompoundTag data = dead.getPersistentData();
        if (!data.hasUUID(RAID_OWNER_KEY)) {
            return;
        }

        UUID owner = data.getUUID(RAID_OWNER_KEY);
        Integer remaining = ACTIVE_RAIDS.get(owner);
        if (remaining == null) {
            return;
        }

        int newRemaining = remaining - 1;
        if (newRemaining > 0) {
            ACTIVE_RAIDS.put(owner, newRemaining);
            return;
        }

        ACTIVE_RAIDS.remove(owner);
        RAID_TIMEOUTS.remove(owner);
        if (!(dead.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }

        ItemStack reward = new ItemStack(com.Momik.usless_mobs.registry.ModItems.AWAKENED_LIVING_CRYSTAL.get());
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.displayClientMessage(Component.translatable("item.usless_mobs.living_crystal.raid_completed")
                .withStyle(ChatFormatting.GREEN), false);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.25F);
    }

    private static void discardRaidMobs(MinecraftServer server, UUID owner) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                CompoundTag data = entity.getPersistentData();
                if (data.hasUUID(RAID_OWNER_KEY) && owner.equals(data.getUUID(RAID_OWNER_KEY))) {
                    entity.discard();
                }
            }
        }
    }
}
