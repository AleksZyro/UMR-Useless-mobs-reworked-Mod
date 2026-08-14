package net.mysith.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;
import org.joml.Vector3f;

/**
 * Glowing-Eyes-Partikel (bei max Soul-Stacks) + dunkler Choral-Ambient-Loop (ab 7+ Stacks).
 */
@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class AuraHandler {

    private static final Map<UUID, Long> lastAmbient = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!ModItems.isReaperScythe(sp.getMainHandItem())
                && !ModItems.isReaperScythe(sp.getOffhandItem())) return;

        int stacks = sp.getPersistentData().getInt("MysithSoulStacks");
        long lastKill = sp.getPersistentData().getLong("MysithLastKill");
        long now = sp.level().getGameTime();
        if (now - lastKill > 200) stacks = 0;

        if (!(sp.level() instanceof ServerLevel sl)) return;

        // === Glowing Eyes: bei stacks == 10 spawnen rote Dust-Partikel auf Augenhöhe ===
        if (stacks >= 10 && now % 2 == 0) {
            DustParticleOptions glow = new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.05F), 0.6F);
            double eyeY = sp.getY() + sp.getEyeHeight() - 0.05;
            // Front offset basierend auf Blickrichtung
            float yaw = (float) Math.toRadians(sp.getYHeadRot());
            double offsetX = -Math.sin(yaw) * 0.15;
            double offsetZ = Math.cos(yaw) * 0.15;
            // Zwei Augen (links + rechts)
            double rightX = Math.cos(yaw) * 0.12;
            double rightZ = Math.sin(yaw) * 0.12;
            sl.sendParticles(glow,
                    sp.getX() + offsetX + rightX, eyeY, sp.getZ() + offsetZ + rightZ,
                    1, 0.01, 0.01, 0.01, 0.0);
            sl.sendParticles(glow,
                    sp.getX() + offsetX - rightX, eyeY, sp.getZ() + offsetZ - rightZ,
                    1, 0.01, 0.01, 0.01, 0.0);
        }

        // === Dynamic Ambient: ab 7 Stacks alle ~5s einen leisen dunklen Drone-Sound ===
        if (stacks >= 7) {
            long last = lastAmbient.getOrDefault(sp.getUUID(), 0L);
            if (now - last >= 100) { // alle 5 Sekunden
                lastAmbient.put(sp.getUUID(), now);
                // Tiefer gedrosselter Wither-Death Sound = düsterer Choral
                float pitch = 0.3F + (stacks - 7) * 0.05F;
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.WITHER_DEATH, SoundSource.AMBIENT, 0.15F, pitch);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.3F, 0.4F);
            }
        }
    }
}
