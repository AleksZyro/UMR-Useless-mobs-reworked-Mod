package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class NetheriteSlimeCoreSwordItem extends SwordItem {
    public static final Tier NETHERITE_SLIME_TIER = new Tier() {
        @Override public int getUses() { return 3200; }
        @Override public float getSpeed() { return 9.0F; }
        @Override public float getAttackDamageBonus() { return 6.0F; }
        @SuppressWarnings("deprecation")
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 28; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(com.Momik.usless_mobs.registry.ModItems.NETHERITE_SCHLEIMKERN.get()); }
    };

    public NetheriteSlimeCoreSwordItem(Properties properties) {
        super(NETHERITE_SLIME_TIER, 5, -1.9F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                        target.getX(), target.getY(0.7D), target.getZ(),
                        14, 0.35D, 0.35D, 0.35D, 0.04D);
            }
        }
        return result;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.netherite_slime_core_sword.tooltip").withStyle(ChatFormatting.GOLD));
    }
}
