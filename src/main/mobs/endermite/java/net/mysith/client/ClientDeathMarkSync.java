package net.mysith.client;

import net.mysith.event.DeathMarkRenderHandler;

public class ClientDeathMarkSync {
    public static void markEntity(int entityId, int durationTicks) {
        DeathMarkRenderHandler.markEntity(entityId, durationTicks);
    }
}
