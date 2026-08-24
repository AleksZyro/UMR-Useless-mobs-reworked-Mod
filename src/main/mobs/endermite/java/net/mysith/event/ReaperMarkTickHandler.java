package net.mysith.event;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModEffects;
import org.joml.Vector3f;

/**
 * Spawns the visual layer for Reaper's Mark: a slow swirl of crimson dust around the marked entity.
 * Damage cadence lives in {@link net.mysith.effect.ReaperMarkEffect}; this is purely cosmetic.
 */
@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class ReaperMarkTickHandler {

    private static final DustParticleOptions CRIMSON_BRIGHT =
            new DustParticleOptions(new Vector3f(0.95F, 0.1F, 0.15F), 0.9F);
    private static final DustParticleOptions CRIMSON_DARK =
            new DustParticleOptions(new Vector3f(0.4F, 0.0F, 0.05F), 1.1F);

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.REAPERS_MARK.get())) return;
        if (!(entity.level() instanceof ServerLevel sl)) return;

        long t = sl.getGameTime();
        // 2 Partikel-Slots pro Tick reichen für sichtbare Aura ohne Server-Last
        for (int i = 0; i < 2; i++) {
            double angle = (t * 0.25 + i * Math.PI) % (Math.PI * 2.0);
            double radius = 0.35 + sl.getRandom().nextDouble() * 0.25;
            double px = entity.getX() + Math.cos(angle) * radius;
            double pz = entity.getZ() + Math.sin(angle) * radius;
            double py = entity.getY() + sl.getRandom().nextDouble() * entity.getBbHeight();
            sl.sendParticles(i == 0 ? CRIMSON_BRIGHT : CRIMSON_DARK,
                    px, py, pz, 1, 0.0, 0.04, 0.0, 0.0);
        }

        // Aufsteigende "Wisp"-Funken alle 8 Ticks
        if (t % 8 == 0) {
            sl.sendParticles(CRIMSON_BRIGHT,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
