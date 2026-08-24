package net.mysith.client;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoulCodexClient {

    // Jedes Array = eine Seite, Werte = Zeilen-Indizes aus den Sprachdateien.
    private static final int[][] PAGE_GROUPS = {
            {2, 3, 4, 5},
            {7, 8, 9, 10, 11, 12},
            {14, 15, 16, 17, 18},
            {20, 21, 22, 23, 24},
            {26, 27, 28, 29, 30, 31, 32},
            {34, 35, 36, 37, 38},
            {40, 41, 42, 43},
            {45, 46, 47, 48, 49, 50},
            {52, 53, 54, 55},
            {57, 58, 59, 60},
            {62, 63, 64, 65, 66, 67, 68, 69},
            {71, 72, 73, 74, 75},
            {78, 79, 80, 81, 82, 83, 84},
            {87, 88, 89, 90, 91, 92, 93, 94},
            {97, 98, 99, 100, 101, 102, 103, 104},
            {107, 108, 109, 110, 111, 112, 113, 114},
            {117, 118, 119, 120, 121, 122, 123, 124},
            {127, 128, 129, 130, 131, 132, 133, 134},
            {137, 138, 139, 140, 141, 142, 143, 144},
            {147, 148, 149, 150, 151, 152, 153, 154}
    };

    public static void openBook() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        ListTag pagesList = new ListTag();

        Language lang = Language.getInstance();
        for (int[] group : PAGE_GROUPS) {
            StringBuilder pageText = new StringBuilder();
            for (int idx : group) {
                String text = lang.getOrDefault("codex.usless_mobs.line" + idx);
                pageText.append(text).append('\n');
            }
            String escaped = pageText.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");
            pagesList.add(StringTag.valueOf("{\"text\":\"" + escaped + "\"}"));
        }

        tag.put("pages", pagesList);
        tag.putString("title", lang.getOrDefault("item.usless_mobs.soul_codex"));
        tag.putString("author", "?");
        tag.putBoolean("resolved", true);

        mc.setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(book)));
    }
}
