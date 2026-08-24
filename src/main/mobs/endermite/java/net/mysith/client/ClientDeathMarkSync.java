package net.mysith.client;

import net.mysith.event.DeathMarkRenderHandler;

public class ClientDeathMarkSync {
    public static void markEntity(int entityId, int durationTicks) {
        if (entityId <= 0 || durationTicks <= 0) {
            return;
        }
        DeathMarkRenderHandler.markEntity(entityId, Math.min(durationTicks, 20 * 60));
    }
}
