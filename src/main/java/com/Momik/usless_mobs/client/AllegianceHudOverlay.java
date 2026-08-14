package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceStage;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, value = Dist.CLIENT)
public final class AllegianceHudOverlay {
    private AllegianceHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        AllegiancePath path = AllegianceUtil.getPath(player);
        AllegianceStage stage = AllegianceUtil.getStage(player);
        if (path == AllegiancePath.NONE && stage == AllegianceStage.NONE) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = screenW - 6;
        int y = 6;

        ChatFormatting pathColor = switch (path) {
            case VOID      -> ChatFormatting.DARK_PURPLE;
            case CELESTIAL -> ChatFormatting.AQUA;
            case LIVING    -> ChatFormatting.GREEN;
            default        -> ChatFormatting.GRAY;
        };

        String pathName = path == AllegiancePath.NONE ? "" : path.name().charAt(0) + path.name().substring(1).toLowerCase();
        String stageName = stage.name().charAt(0) + stage.name().substring(1).toLowerCase().replace('_', ' ');

        Component line1 = Component.literal(pathName.isEmpty() ? stageName : pathName + " · " + stageName)
                .withStyle(pathColor);
        int textWidth1 = mc.font.width(line1);
        gfx.drawString(mc.font, line1, x - textWidth1, y, 0xFFFFFF, true);

        // Show kill progress if in PATH_TRIAL stage
        if (stage == AllegianceStage.PATH_TRIAL) {
            int kills = AllegianceUtil.getPathKills(player);
            int needed = switch (path) {
                case VOID      -> 5;
                case CELESTIAL -> 5;
                case LIVING    -> 3;
                default        -> 5;
            };
            Component line2 = Component.literal("Trial: " + kills + " / " + needed)
                    .withStyle(ChatFormatting.YELLOW);
            int textWidth2 = mc.font.width(line2);
            gfx.drawString(mc.font, line2, x - textWidth2, y + 10, 0xFFFFFF, true);
        } else if (stage == AllegianceStage.MASTERED) {
            int setKills = AllegianceUtil.getSetKills(player);
            Component line2 = Component.literal("Mastery: " + setKills + " / 30")
                    .withStyle(ChatFormatting.GOLD);
            int textWidth2 = mc.font.width(line2);
            gfx.drawString(mc.font, line2, x - textWidth2, y + 10, 0xFFFFFF, true);
        }
    }
}
