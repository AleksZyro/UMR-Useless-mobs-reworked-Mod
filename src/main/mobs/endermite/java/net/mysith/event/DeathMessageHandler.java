package net.mysith.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class DeathMessageHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;

        ItemStack weapon = killer.getMainHandItem();
        if (!ModItems.isReaperScythe(weapon)) return;

        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        // Nur für Spieler-Kills oder Boss-Kills
        boolean isWorthyKill = target instanceof Player
                || target instanceof EnderDragon
                || target instanceof WitherBoss
                || target instanceof Warden
                || target instanceof ElderGuardian
                || target.hasCustomName();

        if (!isWorthyKill) return;

        Component msg = Component.translatable("death.attack.usless_mobs.reaped",
                target.getDisplayName(),
                killer.getDisplayName());

        if (target.level().getServer() != null) {
            target.level().getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }
    }
}
