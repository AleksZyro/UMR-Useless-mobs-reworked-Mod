package com.Momik.usless_mobs.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class SlimeCoreSwordItem extends SwordItem {

    // Tier stats — was weaker than diamond; now between Diamond and Netherite,
    // plus the on-hit slowing pressure makes it elite-tier without annoying knockback.
    public static final Tier SLIME_CORE_TIER = new Tier() {
        @Override
        public int getUses() { return 2400; }                  // was 1800 → more durable

        @Override
        public float getSpeed() { return 8.0F; }               // was 7.5 → slightly faster mining

        @Override
        public float getAttackDamageBonus() { return 5.0F; }   // was 3.0 → significant damage bump

        @Override
        @SuppressWarnings("deprecation")
        public int getLevel() { return 4; }                    // was 3 (diamond level) → now mines like netherite

        @Override
        public int getEnchantmentValue() { return 25; }        // was 20 → better enchantments

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(com.Momik.usless_mobs.registry.ModItems.SCHLEIMKERN.get());
        }
    };

    public SlimeCoreSwordItem(Properties properties) {
        // attackDamage=4 (was 2) + tier bonus 5 + base 1 = 10 total attack damage (Netherite is 8).
        // attackSpeed -2.0 (was -2.4) → faster swing recovery.
        super(SLIME_CORE_TIER, 4, -2.0F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) {
            // Stronger debuff stack on hit — slowness II (was I) for longer, plus weakness.
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        return result;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
