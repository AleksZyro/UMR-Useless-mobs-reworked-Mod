package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends an active snapshot when a player begins tracking the Frost Stray after its cast started. */
@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrostStrayAbilitySyncHandler {
    private FrostStrayAbilitySyncHandler() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof FrostStrayEntity frostStray) {
            frostStray.syncActiveVolleyTo(player);
        }
    }
}
