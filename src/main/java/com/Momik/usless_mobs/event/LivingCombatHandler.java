package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Stray;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class LivingCombatHandler {
    private LivingCombatHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        if (source instanceof CaveSpider) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                        target.getX(), target.getY(0.6D), target.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.02D);
            }
        }

        if (source instanceof Stray) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 120));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }

        if (source instanceof Husk husk) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 180, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0));
            if (husk.getHealth() < husk.getMaxHealth()) {
                husk.heal(1.0F);
            }
            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                        target.getX(), target.getY(0.65D), target.getZ(),
                        10, 0.3D, 0.3D, 0.3D, 0.01D);
            }
        }
    }
}
