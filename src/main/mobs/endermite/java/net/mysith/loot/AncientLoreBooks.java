package net.mysith.loot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public final class AncientLoreBooks {
    private AncientLoreBooks() {
    }

    public enum LoreBook {
        THREE_PATHS("three_paths", "The Three Paths", 2),
        BRIGHT_SLIME("bright_slime", "The Bright Gel", 2),
        CELESTIAL_SLIME("celestial_slime", "The Bent Star", 2),
        CORRUPTED_DEEP("corrupted_deep", "The Stone That Listened", 2),
        LIVING_ROOTS("living_roots", "The Rooted Heart", 2),
        VOID_REAPER("void_reaper", "The Door That Hunts", 2),
        TRUE_CROWN("true_crown", "The Last Crown", 2);

        private final String id;
        private final String title;
        private final int pages;

        LoreBook(String id, String title, int pages) {
            this.id = id;
            this.title = title;
            this.pages = pages;
        }
    }

    public static LoreBook pick(RandomSource random, LoreBook... books) {
        return books[random.nextInt(books.length)];
    }

    public static ItemStack create(LoreBook book) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        ListTag pages = new ListTag();

        for (int page = 1; page <= book.pages; page++) {
            pages.add(StringTag.valueOf("{\"translate\":\"lore.usless_mobs." + book.id + ".page" + page + "\"}"));
        }

        tag.put("pages", pages);
        tag.putString("title", book.title);
        tag.putString("author", "A hand older than villages");
        tag.putInt("generation", 2);
        tag.putBoolean("resolved", false);
        return stack;
    }
}
