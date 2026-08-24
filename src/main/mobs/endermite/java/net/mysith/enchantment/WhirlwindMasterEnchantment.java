package net.mysith.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;
import net.mysith.registry.ModItems;

public class WhirlwindMasterEnchantment extends Enchantment {
    public WhirlwindMasterEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }
    @Override public int getMaxLevel() { return 2; }
    @Override public int getMinCost(int level) { return 18 + (level - 1) * 12; }
    @Override public int getMaxCost(int level) { return getMinCost(level) + 50; }
    @Override public boolean canEnchant(ItemStack stack) { return ModItems.isReaperScythe(stack); }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return ModItems.isReaperScythe(stack); }
}
