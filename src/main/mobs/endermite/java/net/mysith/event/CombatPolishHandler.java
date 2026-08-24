package net.mysith.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class CombatPolishHandler {

    public static final String SOUL_IMPRINT_KEY = "MysithSoulImprint";
    private static final String MULTI_KILL_COUNT_KEY = "MysithMultiKillCount";
    private static final String MULTI_KILL_LAST_KEY = "MysithMultiKillLast";

    /** Last heartbeat tick per player UUID (for cooldown). */
    private static final Map<UUID, Long> lastHeartbeat = new HashMap<>();

    // ===== Death Marker + Multi-Kill Voicelines =====
    @SubscribeEvent
    public static void onScytheKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (!ModItems.isReaperScythe(killer.getMainHandItem())) return;
        if (!(killer.level() instanceof ServerLevel sl)) return;

        LivingEntity target = event.getEntity();

        // === Death Marker: Asche-Würfel-Effekt am Todesort ===
        DustParticleOptions ash = new DustParticleOptions(new Vector3f(0.15F, 0.05F, 0.05F), 1.6F);
        DustParticleOptions ember = new DustParticleOptions(new Vector3f(0.7F, 0.1F, 0.05F), 1.2F);
        double tx = target.getX(), ty = target.getY() + 0.5, tz = target.getZ();
        for (int i = 0; i < 25; i++) {
            sl.sendParticles(ash,
                    tx + (Math.random() - 0.5) * 1.2, ty + Math.random() * 1.2, tz + (Math.random() - 0.5) * 1.2,
                    1, 0.0, 0.02, 0.0, 0.01);
        }
        for (int i = 0; i < 12; i++) {
            sl.sendParticles(ember,
                    tx + (Math.random() - 0.5) * 0.6, ty + Math.random() * 0.8, tz + (Math.random() - 0.5) * 0.6,
                    1, 0.0, 0.05, 0.0, 0.02);
        }
        sl.sendParticles(ParticleTypes.SOUL, tx, ty, tz, 5, 0.3, 0.2, 0.3, 0.02);

        // === Multi-Kill Voicelines ===
        if (killer instanceof ServerPlayer sp) {
            CompoundTag data = sp.getPersistentData();
            long now = sl.getGameTime();
            long lastKill = data.getLong(MULTI_KILL_LAST_KEY);
            int count = data.getInt(MULTI_KILL_COUNT_KEY);
            // Reset wenn > 5 Sek (100 ticks)
            if (now - lastKill > 100) {
                count = 1;
            } else {
                count++;
            }
            data.putInt(MULTI_KILL_COUNT_KEY, count);
            data.putLong(MULTI_KILL_LAST_KEY, now);

            if (count == 3) {
                playReaperVoice(sl, sp, 0.7F, 0.5F, "usless_mobs.reaper_voice.again");
            } else if (count == 5) {
                playReaperVoice(sl, sp, 0.8F, 0.4F, "usless_mobs.reaper_voice.more");
            } else if (count == 7) {
                playReaperVoice(sl, sp, 0.9F, 0.35F, "usless_mobs.reaper_voice.yes");
            } else if (count == 10) {
                playReaperVoice(sl, sp, 1.0F, 0.3F, "usless_mobs.reaper_voice.feast");
            }
        }
    }

    /** Simuliert "Reaper-Voiceline" mit gepitchten Wither-Ambient + Soul-Escape Sounds. */
    private static void playReaperVoice(ServerLevel sl, ServerPlayer player, float volume, float pitch, String translationKey) {
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_AMBIENT, SoundSource.PLAYERS, volume, pitch);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, volume * 0.6F, pitch + 0.15F);

        player.sendSystemMessage(
                Component.translatable(translationKey).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC, ChatFormatting.BOLD)
        );
    }

    // ===== Heartbeat (Curse-Mode-Audio) =====
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!ModItems.isReaperScythe(sp.getMainHandItem())
                && !ModItems.isReaperScythe(sp.getOffhandItem())) return;
        if (sp.getHealth() / sp.getMaxHealth() >= 0.3F) return;

        long now = sp.level().getGameTime();
        long last = lastHeartbeat.getOrDefault(sp.getUUID(), 0L);
        if (now - last < 22) return; // ~1.1 sec zwischen Beats (Herz-Rhythmus)

        lastHeartbeat.put(sp.getUUID(), now);
        ServerLevel sl = (ServerLevel) sp.level();
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // ===== Scythe-Death-Drop mit Soul Imprint =====
    @SubscribeEvent
    public static void onScytheHolderDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer dying)) return;
        // Bei jedem Tod: scanne Inventar nach Sense, präge Soul Imprint ein
        for (int i = 0; i < dying.getInventory().getContainerSize(); i++) {
            ItemStack stack = dying.getInventory().getItem(i);
            if (!ModItems.isReaperScythe(stack)) continue;
            // Soul Imprint setzen falls noch nicht da
            CompoundTag tag = stack.getOrCreateTag();
            if (!tag.contains(SOUL_IMPRINT_KEY)) {
                tag.putString(SOUL_IMPRINT_KEY, dying.getName().getString());
            }
        }
    }
}
