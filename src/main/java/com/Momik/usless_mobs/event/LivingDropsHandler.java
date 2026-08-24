package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.FrostStrayEntity;
import com.Momik.usless_mobs.entity.WebCaveSpiderEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class LivingDropsHandler {

    private LivingDropsHandler() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }

        int looting = Math.max(0, Math.min(5, event.getLootingLevel()));
        if (entity instanceof FrostStrayEntity || entity instanceof WebCaveSpiderEntity || entity instanceof CoralDrownedEntity) {
            return;
        }
        if (entity instanceof Bat) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.BAT_WING.get()), 0.45F + 0.06F * looting);
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.SHADOWTOOTH.get()), 0.12F + 0.035F * looting);
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get().getDefaultInstance(), natureCrystalChance(entity, looting));
            return;
        }
        if (entity instanceof Axolotl) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.AXOLOTL_GILLS.get()), 0.36F + 0.07F * looting);
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get().getDefaultInstance(), natureCrystalChance(entity, looting));
            return;
        }
        if (entity instanceof Husk) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.SHADOWTOOTH.get()), 0.20F + 0.045F * looting);
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get()), 0.28F + 0.055F * looting);
            return;
        }
        if (entity instanceof Drowned) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.CORAL_SCALE.get(), 1 + entity.getRandom().nextInt(1 + Math.max(1, looting + 1))), 0.42F + 0.08F * looting);
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get().getDefaultInstance(), 0.10F + 0.035F * looting);
            return;
        }
        if (entity instanceof GlowSquid) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.GLOW_FLARE.get()), 0.38F + 0.08F * looting);
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get().getDefaultInstance(), natureCrystalChance(entity, looting));
            return;
        }
        if (entity instanceof Squid) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.TENTACLE.get()), 0.28F + 0.06F * looting);
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get().getDefaultInstance(), natureCrystalChance(entity, looting));
            return;
        }
        if (entity instanceof Stray) {
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.FROST_CORE.get().getDefaultInstance(), 0.18F + 0.05F * looting);
            return;
        }
        if (entity instanceof CaveSpider) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get(), 1 + entity.getRandom().nextInt(1 + Math.max(1, looting + 1))), 0.65F + 0.08F * looting);
            return;
        }
        if (entity instanceof Spider) {
            maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.LIVING_TISSUE.get()), 0.32F + 0.06F * looting);
            return;
        }
        if (isNatureCrystalSource(entity)) {
            maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get().getDefaultInstance(), natureCrystalChance(entity, looting));
            if (entity instanceof PolarBear) {
                maybeDrop(entity, new ItemStack(com.Momik.usless_mobs.registry.ModItems.BEAR_CLAW.get(), 1 + entity.getRandom().nextInt(1 + Math.max(1, looting + 1))), 0.70F + 0.08F * looting);
                maybeDrop(entity, com.Momik.usless_mobs.registry.ModItems.BEARCLAW_NECKLACE.get().getDefaultInstance(), 0.06F + 0.02F * looting);
            }
        }
    }

    private static boolean isNatureCrystalSource(LivingEntity entity) {
        if (entity instanceof WaterAnimal || entity instanceof Axolotl || entity instanceof Frog) {
            return true;
        }
        if (entity instanceof Goat || entity instanceof AbstractHorse || entity instanceof Llama || entity instanceof TraderLlama || entity instanceof Sniffer) {
            return false; // avoid punishing utility/rare animals.
        }
        return entity instanceof PolarBear
                || entity.getType() == EntityType.FOX
                || entity.getType() == EntityType.WOLF
                || entity.getType() == EntityType.BAT
                || entity instanceof Animal;
    }

    private static float natureCrystalChance(LivingEntity entity, int looting) {
        float base = entity instanceof PolarBear ? 0.24F : 0.08F;
        return Math.min(0.45F, base + 0.035F * looting);
    }

    private static void maybeDrop(LivingEntity entity, ItemStack stack, float chance) {
        if (!stack.isEmpty() && entity.getRandom().nextFloat() < Math.min(1.0F, chance)) {
            entity.spawnAtLocation(stack);
        }
    }
}
