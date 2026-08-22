package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.entity.HelpingAllayEntity;
import com.Momik.usless_mobs.registry.ModEntities;
import com.Momik.usless_mobs.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.allay.Allay;
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
        if (allay instanceof HelpingAllayEntity) {
            return;
        }
        if (!allay.getPersistentData().getBoolean(HELPED_KEY) || !allay.getPersistentData().hasUUID(OWNER_KEY)) {
            return;
        }
        convertToHelpingAllay(
                serverLevel,
                allay,
                allay.getPersistentData().getUUID(OWNER_KEY),
                allay.getPersistentData().getLong(SUPPORT_UNTIL_KEY));
    }

    @SubscribeEvent
    public static void onAllayInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Allay allay)) {
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (allay instanceof HelpingAllayEntity helpingAllay
                && stack.is(com.Momik.usless_mobs.registry.ModItems.GLOW_FLARE.get())) {
            extendAllaySupport(event, helpingAllay, player, stack);
            return;
        }
        if (!stack.is(Items.AMETHYST_SHARD)
                || allay instanceof HelpingAllayEntity
                || allay.getPersistentData().getBoolean(HELPED_KEY)) {
            return;
        }

        if (!player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {
            long supportUntil = player.level().getGameTime() + 20L * 60L * 8L;
            HelpingAllayEntity helpingAllay = convertToHelpingAllay(
                    serverLevel,
                    allay,
                    player.getUUID(),
                    supportUntil);
            if (helpingAllay == null) {
                return;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            ItemStack reward = new ItemStack(com.Momik.usless_mobs.registry.ModItems.HELPING_SOUL.get());
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                    helpingAllay.getX(), helpingAllay.getY(0.8D), helpingAllay.getZ(),
                    12, 0.25D, 0.25D, 0.25D, 0.0D);
            serverLevel.playSound(null, helpingAllay.blockPosition(), ModSounds.HELPING_ALLAY_BOND.get(), SoundSource.NEUTRAL, 0.9F, 1.25F);
            player.displayClientMessage(Component.translatable("item.usless_mobs.helping_soul.received")
                    .withStyle(ChatFormatting.AQUA), true);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void extendAllaySupport(
            PlayerInteractEvent.EntityInteract event,
            HelpingAllayEntity allay,
            Player player,
            ItemStack stack) {
        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            long now = player.level().getGameTime();
            allay.extendSupport(player.getUUID(), now, 20L * 60L * 5L);
            allay.setCustomName(Component.translatable("entity.usless_mobs.helping_allay"));
            allay.setPersistenceRequired();
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                        allay.getX(), allay.getY(0.8D), allay.getZ(),
                        18, 0.25D, 0.25D, 0.25D, 0.02D);
                serverLevel.playSound(null, allay.blockPosition(), ModSounds.HELPING_ALLAY_BOND.get(), SoundSource.NEUTRAL, 0.8F, 1.35F);
            }
            player.displayClientMessage(Component.translatable("item.usless_mobs.helping_soul.extended")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static HelpingAllayEntity convertToHelpingAllay(
            ServerLevel serverLevel,
            Allay allay,
            java.util.UUID owner,
            long supportUntil) {
        HelpingAllayEntity replacement = ModEntities.HELPING_ALLAY.get().create(serverLevel);
        if (replacement == null) {
            return null;
        }
        copyAllayState(allay, replacement);
        replacement.bind(owner, supportUntil);
        replacement.setCustomName(Component.translatable("entity.usless_mobs.helping_allay"));
        replacement.setPersistenceRequired();
        if (!serverLevel.addFreshEntity(replacement)) {
            return null;
        }
        allay.discard();
        return replacement;
    }

    private static void copyAllayState(Allay source, HelpingAllayEntity target) {
        target.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        target.setYHeadRot(source.getYHeadRot());
        target.setYBodyRot(source.yBodyRot);
        target.setDeltaMovement(source.getDeltaMovement());
        target.setHealth(source.getHealth());
        target.setCustomName(source.getCustomName());
        target.setCustomNameVisible(source.isCustomNameVisible());
        target.setNoAi(source.isNoAi());
        target.setSilent(source.isSilent());
        target.setNoGravity(source.isNoGravity());
        target.setInvulnerable(source.isInvulnerable());
        target.setGlowingTag(source.isCurrentlyGlowing());
        target.setItemInHand(InteractionHand.MAIN_HAND, source.getItemInHand(InteractionHand.MAIN_HAND).copy());
        target.getPersistentData().merge(source.getPersistentData().copy());
        if (source.isPersistenceRequired()) {
            target.setPersistenceRequired();
        }
    }
}
