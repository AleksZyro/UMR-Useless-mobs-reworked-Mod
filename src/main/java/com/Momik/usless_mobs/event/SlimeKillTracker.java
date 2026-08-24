package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Config;
import com.Momik.usless_mobs.entity.BlueSlimeEntity;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class SlimeKillTracker {

    private static final String KILL_COUNT_TAG = "UslessMobs_SlimeKills";
    private static final String LAST_ROLL_DAY_TAG = "UslessMobs_LastKingRollDay";

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        if (killed.level().isClientSide) {
            return;
        }

        if (!(killed instanceof BlueSlimeEntity) && !(killed instanceof KingSlimeEntity) && !(killed instanceof net.minecraft.world.entity.monster.Slime)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int kills = data.getInt(KILL_COUNT_TAG) + 1;
        data.putInt(KILL_COUNT_TAG, kills);

        if (kills == Config.slimeKillThreshold) {
            KingSlimeAdvancements.grant(player, KingSlimeAdvancements.SLIME_HUNTER);
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("usless_mobs.message.king_unlocked"),
                false
            );
            player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        if (event.level.dimension() != Level.OVERWORLD) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        long dayTime = serverLevel.getDayTime();
        long currentDay = dayTime / 24000L;
        long dawnOffset = dayTime % 24000L;
        if (dawnOffset != 0L) return;

        List<ServerPlayer> qualified = new ArrayList<>();
        for (ServerPlayer player : serverLevel.players()) {
            CompoundTag data = player.getPersistentData();
            if (data.getInt(KILL_COUNT_TAG) < Config.slimeKillThreshold) continue;
            if (!KingSlimeAdvancements.hasEnteredNether(player)) continue;
            if (data.getLong(LAST_ROLL_DAY_TAG) >= currentDay) continue;

            data.putLong(LAST_ROLL_DAY_TAG, currentDay);
            qualified.add(player);
        }

        if (qualified.isEmpty()) return;

        ServerPlayer target = qualified.get(serverLevel.getRandom().nextInt(qualified.size()));
        if (serverLevel.getRandom().nextFloat() >= Config.dailySpawnChance) return;

        if (trySpawnKingSomewhere(serverLevel, target)) {
            KingSlimeAdvancements.grant(target, KingSlimeAdvancements.SUMMON_KING_SLIME);
            target.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("usless_mobs.message.king_arrived"),
                true
            );
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
    }

    private static boolean trySpawnKingSomewhere(ServerLevel level, ServerPlayer target) {
        var random = level.getRandom();
        int minDistance = Math.min(Config.minSpawnDistance, Config.maxSpawnDistance);
        int maxDistance = Math.max(Config.minSpawnDistance, Config.maxSpawnDistance);
        for (int attempt = 0; attempt < Config.maxSpawnAttempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = minDistance + random.nextDouble() * (maxDistance - minDistance);
            int dx = (int) (Math.cos(angle) * dist);
            int dz = (int) (Math.sin(angle) * dist);
            int x = target.blockPosition().getX() + dx;
            int z = target.blockPosition().getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos spawnPos = new BlockPos(x, y, z);
            if (!level.getBlockState(spawnPos).getCollisionShape(level, spawnPos).isEmpty()) continue;
            BlockPos below = spawnPos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            if (level.canSeeSky(spawnPos)) {
                spawnKingAt(level, spawnPos);
                return true;
            }
        }
        return false;
    }

    private static void spawnKingAt(ServerLevel level, BlockPos pos) {
        KingSlimeEntity king = com.Momik.usless_mobs.registry.ModEntities.KING_SCHLEIM.get().create(level);
        if (king == null) return;
        king.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        king.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        level.addFreshEntity(king);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, 6, 2.0D, 1.5D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, 120, 2.0D, 2.5D, 2.0D, 0.5D);
    }

    public static int getSlimeKills(Player player) {
        return player.getPersistentData().getInt(KILL_COUNT_TAG);
    }
}
