package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.entity.GiantSquidEntity;
import com.Momik.usless_mobs.registry.ModEntities;
import com.Momik.usless_mobs.worldgen.ModStructures;
import com.Momik.usless_mobs.worldgen.WhaleRuinEncounterData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WhaleRuinEncounterHandler {
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure>
            ANCIENT_WHALE_RUIN_KEY = ModStructures.ANCIENT_WHALE_RUIN_KEY;

    private WhaleRuinEncounterHandler() {
    }

    @SubscribeEvent
    public static void onGoldBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(Blocks.GOLD_BLOCK)) {
            return;
        }
        BlockPos brokenPos = event.getPos();
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(brokenPos, ANCIENT_WHALE_RUIN_KEY);
        if (!start.isValid()) {
            return;
        }

        String encounterKey = encounterKey(level, start);
        WhaleRuinEncounterData data = WhaleRuinEncounterData.get(level.getServer());
        WhaleRuinEncounterData.Encounter encounter = data.getEncounter(encounterKey);
        if (encounter != null && encounter.defeated()) {
            return;
        }

        event.setCanceled(true);
        BoundingBox bounds = start.getBoundingBox();
        BlockPos center = bounds.getCenter();
        boolean activatedNow = data.activateIfInactive(encounterKey, center.asLong());
        if (activatedNow) {
            warnPlayers(level, center);
        }
        encounter = data.getEncounter(encounterKey);
        if (encounter == null || !hasLivingBoss(level, encounter)) {
            spawnBoss(level, center, encounterKey, data);
        }
    }

    private static String encounterKey(ServerLevel level, StructureStart start) {
        return level.dimension().location() + ":" + start.getChunkPos().toLong();
    }

    private static boolean hasLivingBoss(ServerLevel level, WhaleRuinEncounterData.Encounter encounter) {
        if (encounter.bossUuid() == null) {
            return false;
        }
        Entity entity = level.getEntity(encounter.bossUuid());
        return entity instanceof GiantSquidEntity && entity.isAlive();
    }

    private static void spawnBoss(ServerLevel level, BlockPos center, String encounterKey,
                                  WhaleRuinEncounterData data) {
        GiantSquidEntity boss = ModEntities.GIANT_SQUID.get().create(level);
        if (boss == null) {
            return;
        }
        BlockPos spawn = findWaterSpawn(level, center.offset(0, 7, 17));
        boss.moveTo(spawn.getX() + 0.5D, spawn.getY() + 0.5D, spawn.getZ() + 0.5D,
                180.0F, 0.0F);
        boss.setRuinEncounterKey(encounterKey);
        boss.setRuinOrigin(center);
        if (level.addFreshEntity(boss)) {
            data.setBossUuid(encounterKey, boss.getUUID());
        }
    }

    private static BlockPos findWaterSpawn(ServerLevel level, BlockPos preferred) {
        for (int dy = 6; dy >= -6; dy--) {
            BlockPos candidate = preferred.offset(0, dy, 0);
            if (level.getFluidState(candidate).isSource()
                    && level.getFluidState(candidate.above(2)).isSource()) {
                return candidate;
            }
        }
        return preferred;
    }

    private static void warnPlayers(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(42.0D, 24.0D, 42.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, true));
            Vec3 pull = Vec3.atCenterOf(center).subtract(player.position());
            if (pull.lengthSqr() > 0.01D) {
                player.setDeltaMovement(player.getDeltaMovement().add(pull.normalize().scale(0.28D)));
                player.hurtMarked = true;
            }
        }
        level.playSound(null, center, SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 2.4F, 0.55F);
        level.sendParticles(ParticleTypes.SQUID_INK, center.getX() + 0.5D,
                center.getY() + 6.0D, center.getZ() + 0.5D,
                180, 10.0D, 6.0D, 10.0D, 0.04D);
    }
}
