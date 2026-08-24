package net.mysith.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ReaperHud {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("soul_stacks", SOUL_STACKS_OVERLAY);
        event.registerAboveAll("crimson_vignette", CRIMSON_VIGNETTE_OVERLAY);
    }

    private static boolean playerHasScythe(LocalPlayer player) {
        for (ItemStack s : player.getInventory().items) {
            if (ModItems.isReaperScythe(s)) return true;
        }
        return ModItems.isReaperScythe(player.getOffhandItem());
    }

    /** Top-right overlay: soul stack indicators (0-10 mini skulls). */
    private static final IGuiOverlay SOUL_STACKS_OVERLAY = (gui, gfx, partial, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!playerHasScythe(mc.player)) return;

        int stacks = mc.player.getPersistentData().getInt("MysithSoulStacks");
        long lastKill = mc.player.getPersistentData().getLong("MysithLastKill");
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (now - lastKill > 200) stacks = 0; // decay
        if (stacks <= 0) return;

        Font font = mc.font;
        int x = w - 12;
        int y = 4;

        // Draw 10 dots, lit ones are crimson, unlit are dark gray
        int dotSize = 5;
        int gap = 1;
        for (int i = 0; i < 10; i++) {
            int dx = x - (i + 1) * (dotSize + gap);
            int color = i < stacks ? 0xFFDC143C : 0xFF3A1010;
            gfx.fill(dx, y, dx + dotSize, y + dotSize, color);
            // Subtle highlight on active
            if (i < stacks) {
                gfx.fill(dx + 1, y + 1, dx + 2, y + 2, 0xFFFFAAAA);
            }
        }

        String label = "✦ " + stacks + " ✦";
        int labelX = w - font.width(label) - 4;
        int labelY = y + dotSize + 2;
        gfx.drawString(font, Component.literal(label).getString(), labelX, labelY, 0xFFDC143C, true);
    };

    /** Edge vignette overlay: dark crimson at screen borders when HP < 30% with scythe. */
    private static final IGuiOverlay CRIMSON_VIGNETTE_OVERLAY = (gui, gfx, partial, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!playerHasScythe(mc.player)) return;
        float hp = mc.player.getHealth() / mc.player.getMaxHealth();
        if (hp >= 0.3F) return;

        // Pulse intensity based on HP (lower = stronger)
        float intensity = (0.3F - hp) / 0.3F; // 0..1
        float pulse = 0.5F + 0.5F * (float) Math.sin(mc.level.getGameTime() * 0.25);
        float alpha = Math.min(0.85F, 0.35F + intensity * 0.5F * pulse);

        // Side bars (left, right, top, bottom)
        int thickness = (int) (Math.min(w, h) * 0.18F);

        int red = (int) (alpha * 255) << 24 | 0x8B0000;
        // Top
        gfx.fillGradient(0, 0, w, thickness, red, 0x008B0000);
        // Bottom
        gfx.fillGradient(0, h - thickness, w, h, 0x008B0000, red);
        // Left
        gfx.fillGradient(0, 0, thickness, h, red, 0x008B0000);
        // Right
        gfx.fillGradient(w - thickness, 0, w, h, 0x008B0000, red);
    };
}
