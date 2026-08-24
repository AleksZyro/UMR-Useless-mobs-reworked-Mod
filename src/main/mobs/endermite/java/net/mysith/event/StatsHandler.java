package net.mysith.event;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;
import net.mysith.world.ReaperStats;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class StatsHandler {

    private static final String CURRENT_STREAK_KEY = "MysithCurrentStreak";

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (!ModItems.isReaperScythe(killer.getMainHandItem())) return;
        if (!(killer instanceof ServerPlayer sp)) return;

        ReaperStats stats = ReaperStats.get((ServerLevel) sp.level().getServer().overworld());
        ReaperStats.Entry e = stats.getOrCreate(sp.getUUID(), sp.getName().getString());
        e.kills++;

        // Streak tracken
        CompoundTag data = sp.getPersistentData();
        int streak = data.getInt(CURRENT_STREAK_KEY) + 1;
        data.putInt(CURRENT_STREAK_KEY, streak);
        if (streak > e.bestStreak) {
            e.bestStreak = streak;
        }
        stats.setDirty();
    }

    @SubscribeEvent
    public static void onDeathByScythe(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (!ModItems.isReaperScythe(killer.getMainHandItem())) return;
        if (killer.getUUID().equals(victim.getUUID())) return;

        ReaperStats stats = ReaperStats.get((ServerLevel) victim.level().getServer().overworld());
        ReaperStats.Entry e = stats.getOrCreate(victim.getUUID(), victim.getName().getString());
        e.deathsToScythe++;
        stats.setDirty();
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // Streak reset bei eigenem Tod
        sp.getPersistentData().putInt(CURRENT_STREAK_KEY, 0);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.getServer() == null) return;

        ReaperStats stats = ReaperStats.get(sp.getServer().overworld());
        List<ReaperStats.Entry> top = stats.getTopByKills(3);
        if (top.isEmpty()) return;
        // Nur senden wenn mindestens 1 Entry mit Kills > 0
        if (top.get(0).kills <= 0) return;

        sp.sendSystemMessage(Component.literal(""));
        sp.sendSystemMessage(
                Component.translatable("usless_mobs.stats.login_title")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );
        String[] medals = {"§6§l①", "§7§l②", "§c§l③"};
        for (int i = 0; i < top.size(); i++) {
            ReaperStats.Entry e = top.get(i);
            sp.sendSystemMessage(
                    Component.translatable("usless_mobs.stats.login_entry",
                            Component.literal(medals[i]),
                            Component.literal(e.name).withStyle(ChatFormatting.WHITE),
                            e.kills,
                            e.bestStreak)
                            .withStyle(ChatFormatting.GRAY)
            );
        }
        sp.sendSystemMessage(Component.literal(""));
    }
}
