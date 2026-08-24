package com.Momik.usless_mobs.item;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;

public class GoldenSlimeSpawnEggItem extends ForgeSpawnEggItem {

    public GoldenSlimeSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int primaryColor, int secondaryColor, Properties properties) {
        super(type, primaryColor, secondaryColor, properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        applyGoldenTag(stack);
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static ItemStack createStack(GoldenSlimeSpawnEggItem item, int count) {
        ItemStack stack = new ItemStack(item, count);
        applyGoldenTag(stack);
        return stack;
    }

    private static void applyGoldenTag(ItemStack stack) {
        CompoundTag entityTag = new CompoundTag();
        entityTag.putBoolean("Golden", true);
        stack.getOrCreateTag().put("EntityTag", entityTag);
    }
}
