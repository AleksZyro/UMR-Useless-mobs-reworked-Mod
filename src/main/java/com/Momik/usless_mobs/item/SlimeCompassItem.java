package com.Momik.usless_mobs.item;

import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class SlimeCompassItem extends Item {

    private static final int SEARCH_RADIUS_CHUNKS = 16;
    private static final int COOLDOWN_TICKS = 100;

    public SlimeCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        long seed = serverLevel.getSeed();
        ChunkPos playerChunk = player.chunkPosition();
        ChunkPos nearest = findNearestSlimeChunk(seed, playerChunk.x, playerChunk.z);

        if (nearest == null) {
            player.displayClientMessage(
                    Component.translatable("item.usless_mobs.slime_kompass.none"), true);
        } else {
            int targetX = nearest.getMiddleBlockX();
            int targetZ = nearest.getMiddleBlockZ();
            int dx = targetX - (int) player.getX();
            int dz = targetZ - (int) player.getZ();
            int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
            String direction = compassDirection(dx, dz);
            player.displayClientMessage(
                    Component.translatable("item.usless_mobs.slime_kompass.found",
                            targetX, targetZ, dist, direction),
                    false);
            stack.getOrCreateTag().putInt("TargetX", targetX);
            stack.getOrCreateTag().putInt("TargetZ", targetZ);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static ChunkPos findNearestSlimeChunk(long seed, int cx, int cz) {
        for (int r = 0; r <= SEARCH_RADIUS_CHUNKS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    if (isSlimeChunk(seed, cx + dx, cz + dz)) {
                        return new ChunkPos(cx + dx, cz + dz);
                    }
                }
            }
        }
        return null;
    }

    // Vanilla slime-chunk formula (Random seeded from world seed + chunk coords).
    public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
        Random rng = new Random(seed
                + (long) (chunkX * chunkX * 0x4c1906)
                + (long) (chunkX * 0x5ac0db)
                + (long) (chunkZ * chunkZ) * 0x4307a7L
                + (long) (chunkZ * 0x5f24f) ^ 0x3ad8025fL);
        return rng.nextInt(10) == 0;
    }

    private static String compassDirection(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? "E" : "W";
        }
        return dz > 0 ? "S" : "N";
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("TargetX");
    }
}
