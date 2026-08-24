package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.item.TruePathArmorItem;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class TruePathSetBonusHandler {
    private static final float VOID_LIFESTEAL_FRACTION = 0.10F;
    private static final float LIVING_THORNS_FRACTION = 0.12F;

    private TruePathSetBonusHandler() {}

    @SubscribeEvent
    public static void onAttackerHits(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        TruePathArmorItem.Path setPath = TruePathArmorItem.getWornFullSetPath(attacker);
        if (setPath != TruePathArmorItem.Path.VOID) {
            return;
        }
        float heal = event.getAmount() * VOID_LIFESTEAL_FRACTION;
        if (heal > 0.0F && attacker.getHealth() < attacker.getMaxHealth()) {
            attacker.heal(heal);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Player player)) {
            return;
        }
        TruePathArmorItem.Path setPath = TruePathArmorItem.getWornFullSetPath(player);
        if (setPath == null) {
            return;
        }

        switch (setPath) {
            case LIVING -> {
                // Thorn-style reflect — small, only against direct living attackers.
                if (event.getSource().getEntity() instanceof LivingEntity attacker
                        && attacker != player && !player.level().isClientSide) {
                    float reflect = event.getAmount() * LIVING_THORNS_FRACTION;
                    if (reflect > 0.0F) {
                        attacker.hurt(player.damageSources().thorns(player), reflect);
                    }
                    if (player.getHealth() <= player.getMaxHealth() * 0.5F) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
                    }
                }
            }
            case CELESTIAL -> {
                // Reduces fall damage further for the mobility set.
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                    event.setAmount(event.getAmount() * 0.4F);
                }
            }
            case VOID -> {
                // Glass cannon: no defensive bonus, no extra reduction.
            }
        }
    }
}
