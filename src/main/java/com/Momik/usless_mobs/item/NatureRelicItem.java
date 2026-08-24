package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class NatureRelicItem extends Item {
    public enum Relic {
        AXOLOTL_GILLS("item.usless_mobs.axolotl_gills.tooltip", ChatFormatting.AQUA, 30 * 20),
        BAT_WING("item.usless_mobs.bat_wing.tooltip", ChatFormatting.GRAY, 24 * 20),
        SHADOWTOOTH("item.usless_mobs.shadowtooth.tooltip", ChatFormatting.DARK_PURPLE, 42 * 20);

        private final String tooltipKey;
        private final ChatFormatting color;
        private final int cooldownTicks;

        Relic(String tooltipKey, ChatFormatting color, int cooldownTicks) {
            this.tooltipKey = tooltipKey;
            this.color = color;
            this.cooldownTicks = cooldownTicks;
        }
    }

    private final Relic relic;

    public NatureRelicItem(Relic relic, Properties properties) {
        super(properties);
        this.relic = relic;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            applyRelicPower((ServerLevel) level, player);
            player.getCooldowns().addCooldown(this, relic.cooldownTicks);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void applyRelicPower(ServerLevel level, Player player) {
        switch (relic) {
            case AXOLOTL_GILLS -> {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 70 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 18 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6 * 20, 0));
                player.heal(2.0F);
                emit(level, player, ParticleTypes.BUBBLE, SoundEvents.GENERIC_SPLASH, 0.8F, 1.25F);
            }
            case BAT_WING -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 28 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 30 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 20, 0));
                if (!player.onGround()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.28D, 0.0D));
                    player.hurtMarked = true;
                }
                emit(level, player, ParticleTypes.CLOUD, SoundEvents.BAT_TAKEOFF, 0.7F, 1.1F);
            }
            case SHADOWTOOTH -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 25 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12 * 20, 1));
                emit(level, player, ParticleTypes.SCULK_SOUL, SoundEvents.ENDERMAN_TELEPORT, 0.55F, 0.55F);
            }
        }
    }

    private static void emit(ServerLevel level, Player player, net.minecraft.core.particles.ParticleOptions particle,
                             SoundEvent sound, float volume, float pitch) {
        level.sendParticles(particle, player.getX(), player.getY(0.65D), player.getZ(),
                18, 0.35D, 0.35D, 0.35D, 0.03D);
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return relic == Relic.SHADOWTOOTH;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(relic.tooltipKey).withStyle(relic.color));
    }
}
