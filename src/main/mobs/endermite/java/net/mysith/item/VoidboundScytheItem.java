package net.mysith.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
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

public class VoidboundScytheItem extends ScytheItem {
    public VoidboundScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0));
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                        target.getX(), target.getY(0.7D), target.getZ(),
                        18, 0.35D, 0.45D, 0.35D, 0.04D);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.38F, 0.04F, 0.68F), 1.25F),
                        target.getX(), target.getY(0.9D), target.getZ(),
                        14, 0.42D, 0.28D, 0.42D, 0.01D);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                        target.getX(), target.getY(0.55D), target.getZ(),
                        10, 0.30D, 0.20D, 0.30D, 0.08D);
                serverLevel.playSound(null, attacker.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.45F, 0.75F);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.voidbound_scythe.tooltip1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.voidbound_scythe.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.usless_mobs.voidbound_scythe.tooltip3").withStyle(ChatFormatting.DARK_RED));
    }
}
