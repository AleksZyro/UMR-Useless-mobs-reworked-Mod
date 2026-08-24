package net.mysith.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;

/**
 * Verhindert dass ein Spieler eine zweite Sense aufhebt wenn er schon eine hat.
 * Damit ist garantiert: max 1 Sense pro Spieler-Inventar.
 */
@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class ScythePickupHandler {

    private static final Map<UUID, Long> lastWarning = new HashMap<>();

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem().getItem();
        if (!ModItems.isReaperScythe(stack)) return;

        // Hat der Spieler schon eine Sense?
        if (ScytheCraftHandler.playerHasScythe(player)) {
            event.setCanceled(true);

            // Throttle für die Nachricht (max alle 3 Sekunden)
            long now = player.level().getGameTime();
            long last = lastWarning.getOrDefault(player.getUUID(), 0L);
            if (now - last > 60) {
                lastWarning.put(player.getUUID(), now);
                player.sendSystemMessage(
                        Component.translatable("mysith.rejection.no_other")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)
                );
            }
        }
    }
}
