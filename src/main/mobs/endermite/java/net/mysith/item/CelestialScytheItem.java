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

public class CelestialScytheItem extends ScytheItem {
    public CelestialScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY(0.7D), target.getZ(),
                        22, 0.35D, 0.45D, 0.35D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY(0.9D), target.getZ(),
                        12, 0.38D, 0.35D, 0.38D, 0.04D);
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        target.getX(), target.getY(1.05D), target.getZ(),
                        8, 0.28D, 0.24D, 0.28D, 0.02D);
                serverLevel.playSound(null, attacker.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.35F);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.35F, 1.55F);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.celestial_scythe.tooltip1").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.celestial_scythe.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
