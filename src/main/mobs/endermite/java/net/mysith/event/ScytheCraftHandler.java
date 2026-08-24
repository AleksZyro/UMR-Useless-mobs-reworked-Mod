package net.mysith.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.MySithMod;
import net.mysith.registry.ModItems;
import net.mysith.world.ScytheTracker;
import org.joml.Vector3f;

public class ScytheCraftHandler {

    public static final String SCYTHE_GEN_TAG = "ScytheGen";
    public static final String REJECTION_COUNT_KEY = "MysithRejectionCount";
    public static final String LAST_REJECTION_TICK_KEY = "MysithLastRejectionTick";
    /** Minimum ticks between counter-increments. Prevents shift-click cascade death. */
    private static final long REJECTION_COOLDOWN_TICKS = 30L;

    /** Pending refunds: player UUID -> ticks remaining. Processed by ScytheTrackerTicker. */
    public static final Map<UUID, Integer> PENDING_REFUNDS = new HashMap<>();

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> SCYTHE_CURSE_DAMAGE =
            net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.tryBuild(MySithMod.MODID, "scythe_curse")
            );

    public static void tickPendingRefunds(MinecraftServer server) {
        if (PENDING_REFUNDS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> it = PENDING_REFUNDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int remaining = e.getValue() - 1;
            if (remaining <= 0) {
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p != null) {
                    refundScytheMaterials(p);
                    p.sendSystemMessage(
                            Component.translatable("mysith.rejection.refund")
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    );
                    ServerLevel sl = (ServerLevel) p.level();
                    sl.playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 0.8F);
                }
                it.remove();
            } else {
                e.setValue(remaining);
            }
        }
    }

    private static void spawnRejectionEffect(ServerPlayer player) {
        ServerLevel sl = (ServerLevel) player.level();
        double px = player.getX();
        double py = player.getY() + 1.0;
        double pz = player.getZ();

        DustParticleOptions crimson = new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.1F), 1.6F);
        DustParticleOptions darkRed = new DustParticleOptions(new Vector3f(0.45F, 0.0F, 0.05F), 1.4F);

        // Ring-Burst: rote Partikel rund um den Spieler
        for (int i = 0; i < 80; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double r = 0.6 + Math.random() * 1.2;
            double dx = Math.cos(angle) * r;
            double dz = Math.sin(angle) * r;
            double dy = -0.3 + Math.random() * 1.6;
            sl.sendParticles(crimson, px + dx, py + dy, pz + dz,
                    1, 0.05, 0.1, 0.05, 0.02);
        }
        for (int i = 0; i < 40; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double r = 0.3 + Math.random() * 0.8;
            sl.sendParticles(darkRed, px + Math.cos(angle) * r, py + Math.random() * 1.2, pz + Math.sin(angle) * r,
                    1, 0.1, 0.1, 0.1, 0.03);
        }

        // Dunkler Rauch
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                px, py + 0.5, pz, 30, 0.6, 0.6, 0.6, 0.04);

        // Damage-Indicators für extra Drama
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                px, py + 0.8, pz, 12, 0.4, 0.4, 0.4, 0.0);

        // Custom Rejection-Sounds (stumm bis .ogg vorhanden) + Vanilla-Layer
        sl.playSound(null, px, py, pz,
                net.mysith.registry.ModSounds.REJECTION_THUNDER.get(), SoundSource.MASTER, 1.5F, 0.8F);
        sl.playSound(null, px, py, pz,
                net.mysith.registry.ModSounds.REJECTION_VOICE.get(), SoundSource.MASTER, 1.2F, 1.0F);
        sl.playSound(null, px, py, pz,
                SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.7F, 0.6F);
        sl.playSound(null, px, py, pz,
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.5F);
        sl.playSound(null, px, py, pz,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 0.4F);
    }

    /** Scan all online player inventories (incl. cursor). Returns the first player holding a scythe, or null. */
    public static ServerPlayer findHolder(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (playerHasScythe(p)) {
                return p;
            }
        }
        return null;
    }

    /** Total scythe count across all online player inventories + cursors + dropped ItemEntities in all loaded worlds.
     *  Counting dropped entities is critical: ohne diesen Check könnte man die Sense droppen
     *  und eine neue craften, sodass zwei Klingen gleichzeitig existieren. */
    public static int countAllScythes(MinecraftServer server) {
        int total = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            total += countPlayerScythes(p);
        }
        // Dropped ItemEntities (in der Welt liegende Sensen) — auch unseren ScytheItemEntity (extends ItemEntity).
        for (ServerLevel sl : server.getAllLevels()) {
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof ItemEntity ie && ModItems.isReaperScythe(ie.getItem())) {
                    total += ie.getItem().getCount();
                }
            }
        }
        return total;
    }

    /** Give back the 4 Netherite + Nether Star + Soul Crystal + Stick the recipe consumes. */
    public static void refundScytheMaterials(ServerPlayer player) {
        giveOrDrop(player, new ItemStack(Items.NETHERITE_INGOT, 4));
        giveOrDrop(player, new ItemStack(Items.NETHER_STAR, 1));
        giveOrDrop(player, new ItemStack(ModItems.SOUL_CRYSTAL.get(), 1));
        giveOrDrop(player, new ItemStack(Items.STICK, 1));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** Extracts last segment of dimension resource location (e.g. "minecraft:overworld" -> "overworld"). */
    public static String formatDimShort(String dim) {
        if (dim == null || dim.isEmpty()) return "?";
        int colon = dim.indexOf(':');
        return colon >= 0 ? dim.substring(colon + 1) : dim;
    }

    /** Scans a player's inventory, armor/offhand, ender chest, and carried cursor stack for a scythe. */
    public static boolean playerHasScythe(ServerPlayer p) {
        return countPlayerScythes(p) > 0;
    }

    /** Finds any holder OTHER than the given player (incl. cursor). */
    public static ServerPlayer findOtherHolder(MinecraftServer server, ServerPlayer self) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(self.getUUID())) continue;
            if (playerHasScythe(p)) {
                return p;
            }
        }
        return null;
    }

    public static int countPlayerScythes(ServerPlayer player) {
        int total = countContainerScythes(player.getInventory());
        total += countContainerScythes(player.getEnderChestInventory());

        // Cursor (Maus) - bei Crafting-/Smithing-Menus landet das Resultat kurz hier.
        if (player.containerMenu != null) {
            ItemStack carried = player.containerMenu.getCarried();
            if (ModItems.isReaperScythe(carried)) {
                total += carried.getCount();
            }
        }

        return total;
    }

    private static int countContainerScythes(Container container) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ModItems.isReaperScythe(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Sweeps all loaded chunks/levels: removes scythes from chests, barrels, ender chests, ground items. */
    public static int sweepNonPlayerScythes(MinecraftServer server) {
        int removed = 0;
        for (ServerLevel sl : server.getAllLevels()) {
            // 1. ItemEntities (Boden-Drops)
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof ItemEntity itemEntity) {
                    if (ModItems.isReaperScythe(itemEntity.getItem())) {
                        itemEntity.discard();
                        removed++;
                    }
                }
            }

        }

        // 2. BlockEntity-Container in geladenen Chunks rund um Online-Spieler
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel sl = (ServerLevel) player.level();
            ChunkPos center = player.chunkPosition();
            int viewDist = 16; // grosszügig
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    ChunkAccess ca = sl.getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, false);
                    if (!(ca instanceof LevelChunk chunk)) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof Container container) {
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                ItemStack stack = container.getItem(i);
                                if (ModItems.isReaperScythe(stack)) {
                                    container.setItem(i, ItemStack.EMPTY);
                                    be.setChanged();
                                    removed++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Ender-Chests aller Online-Spieler
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerEnderChestContainer ec = p.getEnderChestInventory();
            for (int i = 0; i < ec.getContainerSize(); i++) {
                ItemStack stack = ec.getItem(i);
                if (ModItems.isReaperScythe(stack)) {
                    ec.setItem(i, ItemStack.EMPTY);
                    removed++;
                }
            }
        }

        return removed;
    }

    @Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
    public static class Forge {

        @SubscribeEvent
        public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
            ItemStack crafted = event.getCrafting();
            if (isScytheUpgrade(crafted)) {
                copyScytheData(crafted, event.getInventory());
                return;
            }
            if (!crafted.is(ModItems.SITH_SCYTHE.get())) return;
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            ServerLevel level = (ServerLevel) player.level();
            ScytheTracker tracker = ScytheTracker.get(level);

            // Tracker = Wahrheit. Falls Tracker einen Holder kennt der NICHT der Crafter ist,
            // gilt: Sense existiert irgendwo (möglicherweise im Offline-Inventar des Holders).
            // Plus: lokale Online-Prüfung als Fallback.
            int total = countAllScythes(server);

            UUID trackerHolder = tracker.getHolderUuid();
            boolean trackerSaysOtherHolder = trackerHolder != null
                    && !trackerHolder.equals(player.getUUID())
                    && tracker.getGeneration() > 0;

            MySithMod.LOGGER.debug("[Scythe] Craft event for {}. Online total: {}, Tracker holder: {}",
                    player.getName().getString(), total,
                    trackerHolder != null ? tracker.getHolderName() : "none");

            if (total > 1 || trackerSaysOtherHolder) {
                rejectCraft(crafted, player, server, level, tracker, trackerSaysOtherHolder, total);
                return;
            }

            acceptCraft(crafted, player, server, level, tracker);
        }

        private static boolean isScytheUpgrade(ItemStack crafted) {
            return crafted.is(ModItems.VOIDBOUND_SCYTHE.get())
                    || crafted.is(ModItems.CELESTIAL_SCYTHE.get())
                    || crafted.is(ModItems.BALANCE_SCYTHE.get());
        }

        private static void copyScytheData(ItemStack crafted, Container ingredients) {
            for (int i = 0; i < ingredients.getContainerSize(); i++) {
                ItemStack ingredient = ingredients.getItem(i);
                if (ModItems.isReaperScythe(ingredient) && ingredient.hasTag()) {
                    crafted.setTag(ingredient.getTag().copy());
                    return;
                }
            }
        }

        private static void rejectCraft(ItemStack crafted, ServerPlayer player, MinecraftServer server,
                                        ServerLevel level, ScytheTracker tracker,
                                        boolean trackerSaysOtherHolder, int onlineTotal) {
            MySithMod.LOGGER.debug("[Scythe] REJECTING craft for {} (online total={}, tracker holder={})",
                    player.getName().getString(), onlineTotal,
                    tracker.getHolderUuid() != null ? tracker.getHolderName() : "none");
            crafted.shrink(crafted.getCount());

            long now = level.getGameTime();
            long lastTick = player.getPersistentData().getLong(LAST_REJECTION_TICK_KEY);
            if (now - lastTick < REJECTION_COOLDOWN_TICKS && lastTick > 0) {
                PENDING_REFUNDS.put(player.getUUID(), 30);
                return;
            }
            player.getPersistentData().putLong(LAST_REJECTION_TICK_KEY, now);

            int count = player.getPersistentData().getInt(REJECTION_COUNT_KEY) + 1;
            player.getPersistentData().putInt(REJECTION_COUNT_KEY, count);
            sendRejectionMessage(player, server, tracker, trackerSaysOtherHolder, count);

            spawnRejectionEffect(player);
            if (count >= 3) {
                spawnRejectionEffect(player);
            }

            if (count >= 4) {
                killRejectedCrafter(player, server, level);
                return;
            }
            if (count >= 3) {
                player.sendSystemMessage(
                        Component.translatable("mysith.rejection.lost")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD, ChatFormatting.ITALIC)
                );
                return;
            }

            PENDING_REFUNDS.put(player.getUUID(), 30);
        }

        private static void sendRejectionMessage(ServerPlayer player, MinecraftServer server,
                                                 ScytheTracker tracker, boolean trackerSaysOtherHolder,
                                                 int count) {
            ServerPlayer other = findOtherHolder(server, player);
            String holderName = other != null
                    ? other.getName().getString()
                    : (trackerSaysOtherHolder ? tracker.getHolderName() : null);
            String holderDimension = other != null
                    ? other.level().dimension().location().getPath()
                    : (trackerSaysOtherHolder ? formatDimShort(tracker.getHolderDimension()) : null);
            String tier = String.valueOf(Math.min(count, 4));

            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(
                    Component.literal("✦ ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                            .append(Component.translatable("mysith.rejection.title." + tier).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(Component.literal(" ✦").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            );
            player.sendSystemMessage(
                    Component.translatable("mysith.rejection.subtitle." + tier)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)
            );

            if (holderName != null) {
                player.sendSystemMessage(
                        Component.translatable("mysith.rejection.holder_info",
                                Component.literal(holderName).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                Component.literal(holderDimension != null ? holderDimension : "?")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)
                        ).withStyle(ChatFormatting.GRAY)
                );
            } else {
                player.sendSystemMessage(
                        Component.translatable("mysith.rejection.no_other")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                );
            }
            player.sendSystemMessage(Component.literal(""));
        }

        private static void killRejectedCrafter(ServerPlayer player, MinecraftServer server, ServerLevel level) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("mysith.rejection.death_broadcast",
                            Component.literal(player.getName().getString()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    false);

            net.minecraft.world.damagesource.DamageSource damageSource =
                    new net.minecraft.world.damagesource.DamageSource(
                            level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                                    .getHolderOrThrow(SCYTHE_CURSE_DAMAGE)
                    );
            player.hurt(damageSource, Float.MAX_VALUE);
            player.getPersistentData().putInt(REJECTION_COUNT_KEY, 0);
        }

        private static void acceptCraft(ItemStack crafted, ServerPlayer player, MinecraftServer server,
                                        ServerLevel level, ScytheTracker tracker) {
            int removed = sweepNonPlayerScythes(server);

            player.getPersistentData().putInt(REJECTION_COUNT_KEY, 0);
            tracker.newScytheCrafted(player);
            crafted.getOrCreateTag().putInt(SCYTHE_GEN_TAG, tracker.getGeneration());

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.mysith.registry.ModSounds.SCYTHE_BIND.get(), SoundSource.MASTER, 1.5F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.6F, 1.4F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.0F, 0.5F);

            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("usless_mobs.scythe.binds_to",
                            Component.literal(player.getName().getString()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    false);

            if (removed > 0) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("usless_mobs.scythe.forgotten_blades_removed", removed)
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                        false);
            }
        }
    }
}
