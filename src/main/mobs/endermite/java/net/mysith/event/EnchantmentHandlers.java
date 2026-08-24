package net.mysith.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.network.ModNetworking;
import net.mysith.registry.ModEffects;
import net.mysith.registry.ModEnchantments;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class EnchantmentHandlers {
    private static final int DEATH_MARK_DURATION_TICKS = 200;

    public static int getDeathMarkLevel(ItemStack weapon) {
        return weapon.getEnchantmentLevel(ModEnchantments.DEATH_MARK.get());
    }

    public static void applyDeathMark(ItemStack weapon, LivingEntity target) {
        int deathMarkLevel = getDeathMarkLevel(weapon);
        if (deathMarkLevel <= 0) return;
        if (target.level().isClientSide()) return;

        target.addEffect(new MobEffectInstance(
                ModEffects.DEATH_MARK.get(), DEATH_MARK_DURATION_TICKS, deathMarkLevel - 1,
                false, true, true));
        ModNetworking.sendDeathMark(target, DEATH_MARK_DURATION_TICKS);
    }

    @SubscribeEvent
    public static void onDeathMarkAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        applyDeathMark(attacker.getMainHandItem(), event.getEntity());
    }

    @SubscribeEvent
    public static void onDeathMarkPlayerClick(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        applyDeathMark(event.getEntity().getMainHandItem(), target);
    }

    // Reaping: mehr XP vom Kill
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        Player killer = event.getAttackingPlayer();
        if (killer == null) return;
        ItemStack weapon = killer.getMainHandItem();
        int level = weapon.getEnchantmentLevel(ModEnchantments.REAPING.get());
        if (level <= 0) return;
        int bonus = (int) (event.getOriginalExperience() * (0.5F * level));
        event.setDroppedExperience(event.getDroppedExperience() + bonus);
    }

    // Crimson Edge: Bleeding-DOT auf Hit (via Wither-Effect)
    @SubscribeEvent
    public static void onHurtBleed(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        ItemStack weapon = attacker.getMainHandItem();
        int bleedLevel = weapon.getEnchantmentLevel(ModEnchantments.CRIMSON_EDGE.get());
        if (bleedLevel > 0) {
            // Reaper's Mark: thematischer Ersatz für Wither (gleiche DOT-Cadence, rotes Visual)
            event.getEntity().addEffect(new MobEffectInstance(ModEffects.REAPERS_MARK.get(), 60 * bleedLevel, 0));
        }

        // Death Mark: setzt eigenen DEATH_MARK Effect (Skull-Billboard via DeathMarkRenderHandler)
        // + sofortiger Damage-Bonus auf den auslösenden Hit.
        int deathMarkLevel = getDeathMarkLevel(weapon);
        if (deathMarkLevel > 0) {
            applyDeathMark(weapon, event.getEntity());
            float multiplier = 1.0F + 0.1F * deathMarkLevel;
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}
