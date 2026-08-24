package com.Momik.usless_mobs.compat.curios;

import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import com.Momik.usless_mobs.item.PathCrownItem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class PathCrownCurio implements ICurio {
    private static final String NEXT_GUARD_KEY = "UmrPathCrownNextGuard";

    private final ItemStack stack;
    private final PathCrownItem.Path path;

    public PathCrownCurio(ItemStack stack, PathCrownItem.Path path) {
        this.stack = stack;
        this.path = path;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        if (!(slotContext.entity() instanceof Player player)) {
            return;
        }
        if (SlimePowerToggle.areEffectsDisabled(player)) {
            return;
        }
        if (!AllegianceUtil.hasPath(player, path.allegiance)) {
            // Mismatched allegiance: a tiny visual foil only, no buffs.
            return;
        }

        applyAura(player);
        sendCrownParticles(player);

        if (player.level().isClientSide) {
            return;
        }
        if (player.tickCount % 60 == 0) {
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.WITHER);
            if (path == PathCrownItem.Path.LIVING) {
                player.removeEffect(MobEffects.POISON);
            }
        }

        int now = player.tickCount;
        int nextGuard = player.getPersistentData().getInt(NEXT_GUARD_KEY + "_" + path.name());
        if (player.getHealth() <= player.getMaxHealth() * 0.30F && nextGuard <= now) {
            applyGuard(player);
            player.getPersistentData().putInt(NEXT_GUARD_KEY + "_" + path.name(), now + 1400);
        }
    }

    private void sendCrownParticles(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel) || player.tickCount % 16 != 0) {
            return;
        }
        ParticleOptions particle = switch (path) {
            case VOID -> ParticleTypes.SCULK_SOUL;
            case CELESTIAL -> ParticleTypes.END_ROD;
            case LIVING -> ParticleTypes.COMPOSTER;
        };
        int count = path == PathCrownItem.Path.LIVING ? 8 : path == PathCrownItem.Path.CELESTIAL ? 7 : 6;
        serverLevel.sendParticles(particle, player.getX(), player.getY(1.25D), player.getZ(),
                count, 0.32D, 0.24D, 0.32D, 0.018D);
        if (path == PathCrownItem.Path.LIVING && player.tickCount % 48 == 0) {
            serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.45D), player.getZ(),
                    1, 0.12D, 0.12D, 0.12D, 0.0D);
        }
    }

    private void applyAura(Player player) {
        switch (path) {
            case VOID -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_BOOST, 80, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
            }
            case CELESTIAL -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 80, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.JUMP, 80, 0);
            }
            case LIVING -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 80, 0);
            }
        }
    }

    private void applyGuard(Player player) {
        switch (path) {
            case VOID -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 220, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_BOOST, 160, 1);
            }
            case CELESTIAL -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 280, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 160, 1);
            }
            case LIVING -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 200, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 160, 0);
            }
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) {
        return true;
    }

    @Override
    public DropRule getDropRule(SlotContext slotContext, net.minecraft.world.damagesource.DamageSource source,
                                int lootingLevel, boolean recentlyHit) {
        return DropRule.DEFAULT;
    }
}
