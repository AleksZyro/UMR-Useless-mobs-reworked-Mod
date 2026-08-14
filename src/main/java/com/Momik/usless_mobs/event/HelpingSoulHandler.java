package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class HelpingSoulHandler {
    private static final String HELPED_KEY = "UslessMobsHelpingSoulGiven";
    private static final String OWNER_KEY = "UslessMobsHelpingSoulOwner";
    private static final String SUPPORT_UNTIL_KEY = "UslessMobsHelpingSoulSupportUntil";

    private HelpingSoulHandler() {
    }

    @SubscribeEvent
    public static void onAllayTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Allay allay) || !(allay.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!allay.getPersistentData().getBoolean(HELPED_KEY) || !allay.getPersistentData().hasUUID(OWNER_KEY)) {
            return;
        }

        long now = serverLevel.getGameTime();
        if (allay.getPersistentData().getLong(SUPPORT_UNTIL_KEY) <= now) {
            return;
        }

        UUID ownerId = allay.getPersistentData().getUUID(OWNER_KEY);
        Player owner = serverLevel.getPlayerByUUID(ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        if (allay.distanceToSqr(owner) > 24.0D * 24.0D) {
            allay.teleportTo(owner.getX(), owner.getY() + 0.5D, owner.getZ());
        } else if (allay.distanceToSqr(owner) > 7.0D * 7.0D) {
            allay.getNavigation().moveTo(owner, 1.15D);
        }

        if (allay.tickCount % 40 == 0) {
            int pressuredMonsters = 0;
            for (Monster monster : serverLevel.getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(8.0D),
                    monster -> monster.isAlive() && monster.hasLineOfSight(owner))) {
                pressuredMonsters++;
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
                monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
            if (pressuredMonsters > 0 && allay.tickCount % 80 == 0 && owner.distanceToSqr(allay) < 10.0D * 10.0D) {
                owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 65, 0, true, false, true));
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                        owner.getX(), owner.getY(0.9D), owner.getZ(),
                        9, 0.35D, 0.45D, 0.35D, 0.01D);
                serverLevel.playSound(null, owner.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 0.55F, 1.45F);
            }
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                    allay.getX(), allay.getY(0.8D), allay.getZ(),
                    5, 0.18D, 0.18D, 0.18D, 0.0D);
        }

        if (allay.tickCount % 120 == 0 && owner.distanceToSqr(allay) < 10.0D * 10.0D && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal(1.0F);
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 0, true, false, true));
        }
    }

    @SubscribeEvent
    public static void onAllayInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Allay allay)) {
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (allay.getPersistentData().getBoolean(HELPED_KEY) && stack.is(com.Momik.usless_mobs.registry.ModItems.GLOW_FLARE.get())) {
            extendAllaySupport(event, allay, player, stack);
            return;
        }
        if (!stack.is(Items.AMETHYST_SHARD) || allay.getPersistentData().getBoolean(HELPED_KEY)) {
            return;
        }

        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            allay.getPersistentData().putBoolean(HELPED_KEY, true);
            allay.getPersistentData().putUUID(OWNER_KEY, player.getUUID());
            allay.getPersistentData().putLong(SUPPORT_UNTIL_KEY, player.level().getGameTime() + 20L * 60L * 8L);
            allay.setCustomName(Component.translatable("entity.usless_mobs.helping_allay"));
            allay.setPersistenceRequired();
            ItemStack reward = new ItemStack(com.Momik.usless_mobs.registry.ModItems.HELPING_SOUL.get());
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                        allay.getX(), allay.getY(0.8D), allay.getZ(),
                        12, 0.25D, 0.25D, 0.25D, 0.0D);
                serverLevel.playSound(null, allay.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 1.0F, 1.25F);
            }
            player.displayClientMessage(Component.translatable("item.usless_mobs.helping_soul.received")
                    .withStyle(ChatFormatting.AQUA), true);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void extendAllaySupport(PlayerInteractEvent.EntityInteract event, Allay allay, Player player, ItemStack stack) {
        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            allay.getPersistentData().putUUID(OWNER_KEY, player.getUUID());
            long now = player.level().getGameTime();
            long current = Math.max(now, allay.getPersistentData().getLong(SUPPORT_UNTIL_KEY));
            allay.getPersistentData().putLong(SUPPORT_UNTIL_KEY, current + 20L * 60L * 5L);
            allay.setCustomName(Component.translatable("entity.usless_mobs.helping_allay"));
            allay.setPersistenceRequired();
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                        allay.getX(), allay.getY(0.8D), allay.getZ(),
                        18, 0.25D, 0.25D, 0.25D, 0.02D);
                serverLevel.playSound(null, allay.blockPosition(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 1.0F, 1.3F);
            }
            player.displayClientMessage(Component.translatable("item.usless_mobs.helping_soul.extended")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
