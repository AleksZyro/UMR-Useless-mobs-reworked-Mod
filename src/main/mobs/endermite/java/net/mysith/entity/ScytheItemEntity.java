package net.mysith.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mysith.world.ScytheTracker;

/**
 * Custom item entity for the dropped Sith Scythe.
 * Resistant to fire, lava, cactus, and explosions — but the void still claims it,
 * triggering a global broadcast announcing the blade's return.
 */
public class ScytheItemEntity extends ItemEntity {

    public ScytheItemEntity(EntityType<? extends ScytheItemEntity> type, Level level) {
        super(type, level);
        // KEIN setUnlimitedLifetime() — das friert das Alter ein (-32768) und stoppt die Rotation.
        // Stattdessen setzt ScytheItem.getEntityLifespan() den Lifespan auf MAX_VALUE,
        // sodass das Alter normal hochzählt (Rotation funktioniert) aber nie despawnt wird.
    }

    /** Used by ScytheItem.createEntity when replacing a vanilla ItemEntity. */
    public ScytheItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Schutz vor Lava, Feuer, Kaktus, Explosionen.
        if (source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        // Fallback: falls jemand FELL_OUT_OF_WORLD via hurt() schickt (unwahrscheinlich für Items
        // — vanilla geht durch outOfWorld() — aber Sicherheitsnetz).
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            announceVoidReturn();
            return super.hurt(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void checkBelowWorld() {
        // Vanilla-Items werden bei Y < (minBuildHeight - 64) per checkBelowWorld() direkt discarded
        // (NICHT via hurt). Deshalb müssen wir hier den Broadcast triggern, bevor super ausführt.
        if (this.getY() < this.level().getMinBuildHeight() - 64) {
            announceVoidReturn();
        }
        super.checkBelowWorld();
    }

    private void announceVoidReturn() {
        if (this.level().isClientSide()) return;
        MinecraftServer server = this.level().getServer();
        if (server == null) return;

        // Vorheriger Holder via ScytheTracker (das ist die Quelle der Wahrheit).
        String name = "Someone";
        if (this.level() instanceof ServerLevel sl) {
            ScytheTracker tracker = ScytheTracker.get(sl);
            if (tracker.getHolderName() != null && !tracker.getHolderName().isEmpty()) {
                name = tracker.getHolderName();
            }
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("usless_mobs.scythe.returned_to_void",
                        Component.literal(name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC),
                false);
    }
}
