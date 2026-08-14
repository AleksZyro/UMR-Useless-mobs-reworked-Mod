package net.mysith.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.item.ScytheItem;
import net.mysith.registry.ModItems;

/**
 * Applies a progressive zoom-in when charging the Scythe.
 * Tap (Tier 1, <5 ticks): no FOV change.
 * Hold (Tier 2+, 5+ ticks): FOV gradually narrows, capping at Tier 3.
 * Client-only — registered on the Forge bus.
 */
@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID, value = Dist.CLIENT)
public class ScytheFovHandler {

    /** FOV multiplier at Tier 3 cap (smaller = more zoom). */
    private static final float MAX_ZOOM = 0.82F;

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        // FOV-Zoom komplett deaktiviert. Charge-Feedback kommt aus Partikeln + Sounds + Slowness.
        // Wenn wir den Zoom später optional wieder reinholen, hier branchen.
        if (true) return;

        Player player = event.getPlayer();
        if (!player.isUsingItem()) return;
        ItemStack using = player.getUseItem();
        if (!ModItems.isReaperScythe(using)) return;
        int ticksUsed = player.getTicksUsingItem();
        if (ticksUsed < ScytheItem.TIER_2_TICKS) return;
        float range = ScytheItem.TIER_3_TICKS - ScytheItem.TIER_2_TICKS;
        float progress = Math.min(1.0F, (ticksUsed - ScytheItem.TIER_2_TICKS) / range);
        float zoom = 1.0F - (1.0F - MAX_ZOOM) * progress;
        event.setNewFovModifier(event.getNewFovModifier() * zoom);
    }
}
