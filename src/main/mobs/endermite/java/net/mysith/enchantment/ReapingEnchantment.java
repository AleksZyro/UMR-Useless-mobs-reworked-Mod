package net.mysith.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;
import net.mysith.registry.ModItems;

public class ReapingEnchantment extends Enchantment {
    public ReapingEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getMinCost(int level) { return 10 + (level - 1) * 8; }
    @Override public int getMaxCost(int level) { return getMinCost(level) + 30; }
    @Override public boolean canEnchant(ItemStack stack) { return ModItems.isReaperScythe(stack) || super.canEnchant(stack); }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return ModItems.isReaperScythe(stack) || super.canApplyAtEnchantingTable(stack); }
}
