package net.mysith.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class LivingScytheItem extends ScytheItem {
    private static final float HEAL_PER_HIT = 1.5F;
    private static final float CRIT_HEAL_BONUS = 2.0F;

    public LivingScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!result) {
            return false;
        }

        float heal = HEAL_PER_HIT;
        if (attacker.getHealth() < attacker.getMaxHealth() * 0.5F) {
            heal += CRIT_HEAL_BONUS;
        }
        attacker.heal(heal);

        attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 0));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));

        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    attacker.getX(), attacker.getY(1.1D), attacker.getZ(),
                    8, 0.35D, 0.45D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    target.getX(), target.getY(0.8D), target.getZ(),
                    14, 0.4D, 0.4D, 0.4D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    target.getX(), target.getY(0.9D), target.getZ(),
                    12, 0.38D, 0.28D, 0.38D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.FALLING_NECTAR,
                    attacker.getX(), attacker.getY(1.2D), attacker.getZ(),
                    6, 0.28D, 0.16D, 0.28D, 0.01D);
            serverLevel.playSound(null, attacker.blockPosition(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 0.6F, 1.1F);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.MOSS_PLACE, SoundSource.PLAYERS, 0.45F, 0.8F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.living_scythe.tooltip1").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.living_scythe.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
