package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.registry.ModEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class RabbitTransformationHandler {
    private static final String SIZE_APPLIED_KEY = "UmrRabbitFormSizeApplied";

    private RabbitTransformationHandler() {
    }

    public static boolean isTransformed(Player player) {
        // EntityEvent.Size also fires from Entity's base constructor, before
        // LivingEntity has created its activeEffects map. Only query effects
        // once Forge has actually attached the player to a world.
        return player.isAddedToWorld() && player.hasEffect(ModEffects.RABBIT_FORM.get());
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && isTransformed(player)) {
            event.setNewSize(EntityDimensions.scalable(0.45F, 0.60F));
            event.setNewEyeHeight(0.48F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        boolean active = isTransformed(player);
        boolean sizeApplied = player.getPersistentData().getBoolean(SIZE_APPLIED_KEY);
        if (active != sizeApplied) {
            player.getPersistentData().putBoolean(SIZE_APPLIED_KEY, active);
            player.refreshDimensions();
        }
        if (active && player.isUsingItem()) {
            player.stopUsingItem();
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (isTransformed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isTransformed(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isTransformed(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (isTransformed(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            clear(player);
        }
    }

    public static void clear(Player player) {
        if (player.removeEffect(ModEffects.RABBIT_FORM.get())) {
            player.getPersistentData().putBoolean(SIZE_APPLIED_KEY, false);
            player.refreshDimensions();
        }
    }
}
