package com.Momik.usless_mobs.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.mysith.registry.ModItems;

public enum CrystalToolTier implements Tier {
    // Balance is the mid-tier transition gear. Each True-path tier wins in its specialty.
    BALANCE(4, 2700, 11.0F, 5.5F, 22),
    VOID(4, 2500, 11.5F, 7.0F, 25),       // glass cannon: highest dmg, lowest uses
    CELESTIAL(4, 2900, 12.5F, 6.0F, 26),  // mobility: fastest harvest
    LIVING(4, 3400, 11.0F, 6.0F, 24);     // tank: highest durability

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;

    CrystalToolTier(int level, int uses, float speed, float damage, int enchantmentValue) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return switch (this) {
            case BALANCE -> Ingredient.of(com.Momik.usless_mobs.registry.ModItems.BALANCE_CATALYST.get(), Items.NETHERITE_INGOT);
            case VOID -> Ingredient.of(ModItems.VOID_CORE.get(), Items.NETHERITE_INGOT);
            case CELESTIAL -> Ingredient.of(ModItems.CELESTIAL_CRYSTAL.get(), Items.NETHERITE_INGOT);
            case LIVING -> Ingredient.of(com.Momik.usless_mobs.registry.ModItems.AWAKENED_LIVING_CRYSTAL.get(), Items.NETHERITE_INGOT);
        };
    }
}
