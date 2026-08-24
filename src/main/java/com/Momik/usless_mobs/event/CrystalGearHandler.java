package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class CrystalGearHandler {
    private CrystalGearHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            applyWeaponEffect(attacker, event.getEntity(), attacker.getMainHandItem());
            applyCrownStrike(attacker, event.getEntity(), event);
        }

        if (event.getEntity() instanceof Player defender && defender.isBlocking()) {
            ItemStack shield = defender.getUseItem();
            if (isVoidShield(shield)) {
                event.setAmount(event.getAmount() * 0.82F);
                defender.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, true));
            } else if (isCelestialShield(shield)) {
                event.setAmount(event.getAmount() * 0.78F);
                defender.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false, true));
            } else if (isBalanceShield(shield)) {
                event.setAmount(event.getAmount() * 0.70F);
                defender.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                defender.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, false, true));
            }
            if (AllegianceUtil.hasPath(defender, AllegiancePath.CELESTIAL)) {
                event.setAmount(event.getAmount() * 0.86F);
                defender.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 70, 0, true, false, true));
                sendParticles(defender, defender, ParticleTypes.END_ROD);
            }
        }

        if (event.getEntity() instanceof Player defender && AllegianceUtil.hasPath(defender, AllegiancePath.VOID)
                && event.getSource().getDirectEntity() instanceof Projectile projectile
                && defender.getRandom().nextFloat() < 0.22F) {
            event.setAmount(0.0F);
            projectile.discard();
            defender.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0, true, false, true));
            sendParticles(defender, defender, ParticleTypes.SCULK_SOUL);
        }
    }

    private static void applyWeaponEffect(Player attacker, LivingEntity target, ItemStack stack) {
        if (isVoidTool(stack)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, true, true, true));
            sendParticles(attacker, target, ParticleTypes.SCULK_SOUL);
        } else if (isCelestialTool(stack)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, true, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false, true));
            sendParticles(attacker, target, ParticleTypes.END_ROD);
        } else if (isBalanceTool(stack)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, true, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 0, true, false, true));
            sendParticles(attacker, target, ParticleTypes.WAX_ON);
        }
    }

    private static void applyCrownStrike(Player attacker, LivingEntity target, LivingHurtEvent event) {
        ItemStack helmet = attacker.getInventory().armor.get(3);
        if (helmet.is(com.Momik.usless_mobs.registry.ModItems.TRUE_VOID_HELMET.get())) {
            event.setAmount(event.getAmount() + 3.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 110, 1, true, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0, true, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, true, true, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 70, 0, true, false, true));
            sendParticles(attacker, target, ParticleTypes.SCULK_SOUL);
        } else if (helmet.is(com.Momik.usless_mobs.registry.ModItems.TRUE_CELESTIAL_HELMET.get())) {
            event.setAmount(event.getAmount() + 2.0F);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 150, 0, true, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1, true, true, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 1, true, false, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90, 1, true, false, true));
            sendParticles(attacker, target, ParticleTypes.END_ROD);
        } else if (helmet.is(com.Momik.usless_mobs.registry.ModItems.TRUE_LIVING_HELMET.get())) {
            event.setAmount(event.getAmount() + 1.5F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 130, 2, true, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, 1, true, true, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, true, false, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false, true));
            if (attacker.getHealth() < attacker.getMaxHealth()) {
                attacker.heal(1.0F);
            }
            sendParticles(attacker, target, ParticleTypes.COMPOSTER);
        }
    }

    private static void sendParticles(Player attacker, LivingEntity target, net.minecraft.core.particles.ParticleOptions particle) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, target.getX(), target.getY(0.65D), target.getZ(),
                    12, 0.3D, 0.35D, 0.3D, 0.03D);
        }
    }

    private static boolean isVoidTool(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_AXE.get()) || stack.is(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_PICKAXE.get())
                || stack.is(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_SHOVEL.get()) || stack.is(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_HOE.get());
    }

    private static boolean isCelestialTool(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_AXE.get()) || stack.is(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_PICKAXE.get())
                || stack.is(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_SHOVEL.get()) || stack.is(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_HOE.get());
    }

    private static boolean isBalanceTool(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.BALANCE_AXE.get()) || stack.is(com.Momik.usless_mobs.registry.ModItems.BALANCE_PICKAXE.get())
                || stack.is(com.Momik.usless_mobs.registry.ModItems.BALANCE_SHOVEL.get()) || stack.is(com.Momik.usless_mobs.registry.ModItems.BALANCE_HOE.get());
    }

    private static boolean isVoidShield(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.VOIDBOUND_SHIELD.get());
    }

    private static boolean isCelestialShield(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.CELESTIAL_SHIELD.get());
    }

    private static boolean isBalanceShield(ItemStack stack) {
        return stack.is(com.Momik.usless_mobs.registry.ModItems.BALANCE_SHIELD.get());
    }
}
