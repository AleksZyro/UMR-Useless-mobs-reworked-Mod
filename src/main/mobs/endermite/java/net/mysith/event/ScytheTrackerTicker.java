package net.mysith.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.world.ScytheTracker;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class ScytheTrackerTicker {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        // Pending refunds jeden Tick verarbeiten (damit der Timing-Effekt sauber ist)
        ScytheCraftHandler.tickPendingRefunds(server);

        if (server.getTickCount() % 20 != 0) return; // Rest alle 1 Sekunde

        // Stack-Cap: Sense darf nicht stacken (auch nicht in Enderchest oder am Cursor).
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            normalizeScytheStacks(p.getInventory());
            normalizeScytheStacks(p.getEnderChestInventory());
            if (p.containerMenu != null) {
                net.minecraft.world.item.ItemStack carried = p.containerMenu.getCarried();
                if (net.mysith.registry.ModItems.isReaperScythe(carried) && carried.getCount() > 1) {
                    carried.setCount(1);
                }
            }
        }

        // Holder-Tracker updaten
        ServerPlayer holder = ScytheCraftHandler.findHolder(server);
        if (holder == null) return;

        ScytheTracker tracker = ScytheTracker.get(server.overworld());
        tracker.updateHolder(holder);
    }

    private static void normalizeScytheStacks(net.minecraft.world.Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = container.getItem(i);
            if (net.mysith.registry.ModItems.isReaperScythe(stack) && stack.getCount() > 1) {
                stack.setCount(1);
                container.setChanged();
            }
        }
    }
}
