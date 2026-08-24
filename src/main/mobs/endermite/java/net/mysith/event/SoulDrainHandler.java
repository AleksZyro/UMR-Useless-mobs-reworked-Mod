package net.mysith.event;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModEnchantments;
import net.mysith.registry.ModSounds;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class SoulDrainHandler {

    private static final DustParticleOptions HEAL_DUST =
            new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.1F), 1.0F);
    private static final DustParticleOptions HEAL_DUST_BRIGHT =
            new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.25F), 1.2F);

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;

        ItemStack weapon = killer.getMainHandItem();
        int level = weapon.getEnchantmentLevel(ModEnchantments.SOUL_DRAIN.get());
        if (level <= 0) return;

        float heal = level * 1.5F;
        killer.heal(heal);

        if (!(killer.level() instanceof ServerLevel sl)) return;

        LivingEntity victim = event.getEntity();
        Vec3 from = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        Vec3 to = killer.position().add(0, killer.getBbHeight() * 0.5, 0);
        int steps = 10 + level * 4;

        // Strom roter Funken vom Opfer zum Killer
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = from.x + (to.x - from.x) * t;
            double py = from.y + (to.y - from.y) * t + Math.sin(t * Math.PI) * 0.3;
            double pz = from.z + (to.z - from.z) * t;
            sl.sendParticles(HEAL_DUST_BRIGHT, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
        }

        // Heal-Aura um Killer
        for (int i = 0; i < 8 + level * 2; i++) {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = 0.3 + sl.getRandom().nextDouble() * 0.5;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            double dy = sl.getRandom().nextDouble() * killer.getBbHeight();
            sl.sendParticles(HEAL_DUST, killer.getX() + dx, killer.getY() + dy, killer.getZ() + dz,
                    1, 0.0, 0.05, 0.0, 0.02);
        }

        // Aufsteigende Herzen-äquivalente: rote Crit-Funken nach oben
        sl.sendParticles(ParticleTypes.HEART,
                killer.getX(), killer.getY() + killer.getBbHeight() + 0.3, killer.getZ(),
                level, 0.2, 0.2, 0.2, 0.0);

        // Custom Heal-Sound (stumm bis .ogg vorhanden)
        sl.playSound(null, killer.getX(), killer.getY(), killer.getZ(),
                ModSounds.SOUL_DRAIN_HEAL.get(), SoundSource.PLAYERS, 0.8F, 1.0F + sl.getRandom().nextFloat() * 0.2F);
    }
}
