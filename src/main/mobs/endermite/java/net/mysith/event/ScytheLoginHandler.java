package net.mysith.event;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.MySithMod;
import net.mysith.world.ScytheTracker;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class ScytheLoginHandler {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getServer() == null) return;

        ScytheTracker tracker = ScytheTracker.get(player.getServer().overworld());
        UUID trackerHolder = tracker.getHolderUuid();

        if (trackerHolder == null) return;
        if (!trackerHolder.equals(player.getUUID())) return;

        // Dieser Spieler ist der registrierte Holder. Hat er die Sense noch?
        if (!ScytheCraftHandler.playerHasScythe(player)) {
            // Holder hat die Sense verloren während offline (gestorben? gelöscht?)
            // Tracker freigeben → andere können wieder craften
            MySithMod.LOGGER.debug("[Scythe] Holder {} logged in WITHOUT scythe. Clearing tracker.",
                    player.getName().getString());
            tracker.clearHolder();

            player.sendSystemMessage(
                    Component.translatable("usless_mobs.scythe.lost_track")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
            );
        } else {
            MySithMod.LOGGER.debug("[Scythe] Holder {} logged in WITH scythe. Tracker intact.",
                    player.getName().getString());
        }
    }
}
