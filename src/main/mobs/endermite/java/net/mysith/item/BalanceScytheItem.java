package net.mysith.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

public class BalanceScytheItem extends ScytheItem {
    public BalanceScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        target.getX(), target.getY(0.8D), target.getZ(),
                        18, 0.45D, 0.45D, 0.45D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY(0.8D), target.getZ(),
                        12, 0.45D, 0.45D, 0.45D, 0.04D);
                serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                        target.getX(), target.getY(0.75D), target.getZ(),
                        12, 0.42D, 0.32D, 0.42D, 0.035D);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.95F, 0.72F, 0.22F), 1.1F),
                        target.getX(), target.getY(1.05D), target.getZ(),
                        16, 0.36D, 0.24D, 0.36D, 0.01D);
                serverLevel.playSound(null, attacker.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 0.75F);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.45F, 1.1F);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.balance_scythe.tooltip1").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.balance_scythe.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
