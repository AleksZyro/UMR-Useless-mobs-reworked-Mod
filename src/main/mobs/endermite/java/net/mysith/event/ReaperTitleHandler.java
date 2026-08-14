package net.mysith.event;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.MySithMod;
import net.mysith.registry.ModItems;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class ReaperTitleHandler {

    private static final String REAPER_KILLS_KEY = "MysithReaperKills";

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (!ModItems.isReaperScythe(killer.getMainHandItem())) return;

        CompoundTag data = killer.getPersistentData();
        int kills = data.getInt(REAPER_KILLS_KEY);
        int newKills = kills + 1;
        data.putInt(REAPER_KILLS_KEY, newKills);

        // Bei 1000 Kills: announce title-unlock
        if (newKills == 1000 && killer.level().getServer() != null) {
            killer.level().getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("usless_mobs.title.reaper_unlocked",
                            Component.literal(killer.getName().getString()).withStyle(ChatFormatting.RED),
                            newKills).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    false);
        }

        // Hidden Advancements bei Meilensteinen
        if (killer instanceof ServerPlayer sp) {
            if (newKills == 50)   grantAdvancement(sp, "kills/initiate");
            if (newKills == 250)  grantAdvancement(sp, "kills/slayer");
            if (newKills == 1000) grantAdvancement(sp, "kills/reaper");
            if (newKills == 5000) grantAdvancement(sp, "kills/soul_reaper");
        }
    }

    private static void grantAdvancement(ServerPlayer player, String path) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Advancement adv = server.getAdvancements().getAdvancement(ResourceLocation.tryBuild(MySithMod.MODID, path));
        if (adv == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(adv, criterion);
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        // Titel ist nur aktiv solange der Spieler die Sense im Inventar hat
        if (!(player instanceof ServerPlayer sp)) return;
        if (!ScytheCraftHandler.playerHasScythe(sp)) return;

        int kills = player.getPersistentData().getInt(REAPER_KILLS_KEY);
        Component prefix = getTitlePrefix(kills);
        if (prefix == null) return;

        // Nur Prefix vor die Nachricht hängen, vanilla zeigt den Namen schon
        Component newMessage = prefix.copy()
                .append(Component.literal(" "))
                .append(event.getMessage());
        event.setMessage(newMessage);
    }

    private static Component getTitlePrefix(int kills) {
        if (kills >= 5000) return Component.translatable("usless_mobs.title.soul_reaper").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        if (kills >= 1000) return Component.translatable("usless_mobs.title.reaper").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        if (kills >= 250) return Component.translatable("usless_mobs.title.slayer").withStyle(ChatFormatting.GOLD);
        if (kills >= 50) return Component.translatable("usless_mobs.title.initiate").withStyle(ChatFormatting.GRAY);
        return null;
    }

    public static int getKills(Player player) {
        return player.getPersistentData().getInt(REAPER_KILLS_KEY);
    }
}
