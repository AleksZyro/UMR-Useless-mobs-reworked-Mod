package net.mysith.silverfish;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class CorruptedSilverfishTracker {
    public static final int DEFAULT_RESONATOR_RADIUS = 48;
    private static final DustParticleOptions CORRUPTION_DUST =
            new DustParticleOptions(new Vector3f(0.45F, 0.10F, 0.75F), 1.15F);

    private CorruptedSilverfishTracker() {}

    public static SearchResult findNearest(ServerLevel level, Vec3 origin, int radius) {
        radius = Mth.clamp(radius, 1, 64);
        SearchResult entityResult = findNearestEntity(level, origin, radius);
        SearchResult hiddenResult = findNearestHiddenHost(level, origin, radius);

        if (entityResult == null) {
            return hiddenResult;
        }
        if (hiddenResult == null) {
            return entityResult;
        }
        return entityResult.distanceSqr() <= hiddenResult.distanceSqr() ? entityResult : hiddenResult;
    }

    public static SearchResult findNearest(ServerLevel level, LivingEntity seeker, int radius) {
        return findNearest(level, seeker.position(), radius);
    }

    private static SearchResult findNearestEntity(ServerLevel level, Vec3 origin, int radius) {
        AABB bounds = new AABB(origin, origin).inflate(radius);
        List<CorruptedSilverfishEntity> entities = level.getEntitiesOfClass(
                CorruptedSilverfishEntity.class,
                bounds,
                entity -> entity.isAlive());

        return entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
                .map(entity -> SearchResult.entity(entity.blockPosition(), entity.distanceToSqr(origin)))
                .orElse(null);
    }

    private static SearchResult findNearestHiddenHost(ServerLevel level, Vec3 origin, int radius) {
        BlockPos center = BlockPos.containing(origin);
        int radiusSqr = radius * radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
        SearchResult best = null;
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                candidate.set(center.getX() + x, center.getY(), center.getZ() + z);
                if (!level.hasChunk(SectionPos.blockToSectionCoord(candidate.getX()), SectionPos.blockToSectionCoord(candidate.getZ()))) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    int dy = y - center.getY();
                    int distSqrBlock = x * x + dy * dy + z * z;
                    if (distSqrBlock > radiusSqr) {
                        continue;
                    }

                    candidate.set(center.getX() + x, y, center.getZ() + z);
                    if (!isCorruptedHost(level, candidate)) {
                        continue;
                    }

                    double distSqr = Vec3.atCenterOf(candidate).distanceToSqr(origin);
                    if (best == null || distSqr < best.distanceSqr()) {
                        best = SearchResult.hidden(candidate.immutable(), distSqr);
                    }
                }
            }
        }

        return best;
    }

    public static boolean isCorruptedHost(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isHostBlock(state)) {
            return false;
        }

        if (isInfestedBlock(state)) {
            return seededChance(level, pos, 5);
        }

        if (pos.getY() > 48 || level.canSeeSky(pos.above())) {
            return false;
        }

        int light = level.getMaxLocalRawBrightness(pos);
        int chance = pos.getY() < 0 ? 75 : 115;
        if (light <= 4) {
            chance = Math.max(48, chance - 24);
        }

        return seededChance(level, pos, chance);
    }

    public static boolean isHostBlock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || isInfestedBlock(state);
    }

    private static boolean isInfestedBlock(BlockState state) {
        return state.is(Blocks.INFESTED_STONE)
                || state.is(Blocks.INFESTED_DEEPSLATE)
                || state.is(Blocks.INFESTED_COBBLESTONE)
                || state.is(Blocks.INFESTED_STONE_BRICKS)
                || state.is(Blocks.INFESTED_MOSSY_STONE_BRICKS)
                || state.is(Blocks.INFESTED_CRACKED_STONE_BRICKS)
                || state.is(Blocks.INFESTED_CHISELED_STONE_BRICKS);
    }

    private static boolean seededChance(ServerLevel level, BlockPos pos, int oneIn) {
        long seed = level.getSeed()
                ^ (pos.asLong() * 0x9E3779B97F4A7C15L)
                ^ 0x51C0B5EEDL;
        return RandomSource.create(seed).nextInt(oneIn) == 0;
    }

    public static CorruptedSilverfishEntity spawnFromHost(ServerLevel level, BlockPos pos, LivingEntity target) {
        CorruptedSilverfishEntity silverfish = com.Momik.usless_mobs.registry.ModEntities.CORRUPTED_SILVERFISH.get().create(level);
        if (silverfish == null) {
            return null;
        }

        silverfish.moveTo(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        silverfish.setPersistenceRequired();
        if (target != null && target.isAlive()) {
            silverfish.setTarget(target);
        }

        if (!level.addFreshEntity(silverfish)) {
            return null;
        }
        reveal(level, pos, true);
        return silverfish;
    }

    public static void reveal(ServerLevel level, BlockPos pos, boolean burst) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.55D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(CORRUPTION_DUST, x, y, z, burst ? 34 : 14, 0.35D, 0.35D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, burst ? 10 : 4, 0.28D, 0.20D, 0.28D, 0.01D);
        level.playSound(null, pos, burst ? SoundEvents.SCULK_SHRIEKER_SHRIEK : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, burst ? 0.85F : 0.35F, burst ? 1.45F : 1.15F);
    }

    public static int vibrationInterval(double distance) {
        return Mth.clamp((int) Math.ceil(distance / 3.0D), 2, 18);
    }

    public record SearchResult(BlockPos pos, boolean hidden, double distanceSqr) {
        public static SearchResult hidden(BlockPos pos, double distanceSqr) {
            return new SearchResult(pos, true, distanceSqr);
        }

        public static SearchResult entity(BlockPos pos, double distanceSqr) {
            return new SearchResult(pos, false, distanceSqr);
        }

        public double distance() {
            return Math.sqrt(distanceSqr);
        }
    }
}
