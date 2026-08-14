package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class GlowFlareItem extends Item {
    public GlowFlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            AABB area = player.getBoundingBox().inflate(12.0D);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive() && mob.getTarget() == player)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 18 * 20, 0));
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8 * 20, 0));
            }
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 35 * 20, 0));
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                    player.getX(), player.getY(0.8D), player.getZ(),
                    36, 0.8D, 0.65D, 0.8D, 0.05D);
            level.playSound(null, player.blockPosition(), SoundEvents.GLOW_SQUID_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.35F);
            player.getCooldowns().addCooldown(this, 18 * 20);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.glow_flare.tooltip").withStyle(ChatFormatting.AQUA));
    }
}
