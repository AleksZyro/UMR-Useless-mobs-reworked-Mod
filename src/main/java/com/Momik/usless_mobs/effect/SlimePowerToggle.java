package com.Momik.usless_mobs.effect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class SlimePowerToggle {
    public static final String EFFECTS_DISABLED_KEY = "UslessMobs_SlimeEffectsDisabled";
    public static final String DISABLED_EFFECT_IDS_KEY = "UslessMobs_DisabledEffectIds";

    private SlimePowerToggle() {}

    // ---- global toggle (K key) -------------------------------------------------

    public static boolean areEffectsDisabled(Player player) {
        return player.getPersistentData().getBoolean(EFFECTS_DISABLED_KEY);
    }

    public static void toggle(Player player) {
        CompoundTag data = player.getPersistentData();
        boolean disabled = !data.getBoolean(EFFECTS_DISABLED_KEY);
        data.putBoolean(EFFECTS_DISABLED_KEY, disabled);

        if (disabled) {
            clearAllManagedEffects(player);
        }

        player.displayClientMessage(Component.translatable(disabled
                ? "message.usless_mobs.slime_effects.disabled"
                : "message.usless_mobs.slime_effects.enabled").withStyle(disabled ? ChatFormatting.RED : ChatFormatting.GREEN), true);
    }

    /**
     * Centralized effect application that respects both the global toggle and the per-effect
     * blocklist. Use this from every armor/curio tick handler so a single check governs them all.
     *
     * Server-only — the disable list lives in server-side persistent data, so if we ran on the
     * client too it would bypass the filter (client has no copy of the disabled list) and apply
     * the effect locally, which makes the player jump higher even though the server thinks the
     * effect is off.
     */
    public static void applyFiltered(Player player, MobEffect effect, int duration, int amplifier) {
        if (player.level().isClientSide) {
            return;
        }
        if (isEffectDisabled(player, effect)) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    // ---- per-effect disable list (command-driven) ------------------------------

    /** True if this specific effect is disabled (either globally or via per-effect list). */
    public static boolean isEffectDisabled(Player player, MobEffect effect) {
        if (areEffectsDisabled(player)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) {
            return false;
        }
        return containsId(player, id);
    }

    /** Adds an effect ID to the per-effect blocklist and removes any live instance. Returns true if the list changed. */
    public static boolean disableEffectId(Player player, ResourceLocation id) {
        ListTag list = getList(player);
        if (containsString(list, id.toString())) {
            return false;
        }
        list.add(StringTag.valueOf(id.toString()));
        player.getPersistentData().put(DISABLED_EFFECT_IDS_KEY, list);
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect != null) {
            player.removeEffect(effect);
        }
        return true;
    }

    /** Removes an effect ID from the per-effect blocklist. Returns true if the list changed. */
    public static boolean enableEffectId(Player player, ResourceLocation id) {
        ListTag list = getList(player);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getAsString().equals(id.toString())) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            player.getPersistentData().put(DISABLED_EFFECT_IDS_KEY, list);
        }
        return removed;
    }

    public static List<ResourceLocation> getDisabledEffectIds(Player player) {
        ListTag list = getList(player);
        List<ResourceLocation> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation rl = ResourceLocation.tryParse(list.get(i).getAsString());
            if (rl != null) {
                out.add(rl);
            }
        }
        return out;
    }

    /** Wipes the per-effect blocklist (does NOT touch the global toggle). */
    public static void clearDisabledEffectIds(Player player) {
        player.getPersistentData().put(DISABLED_EFFECT_IDS_KEY, new ListTag());
    }

    // ---- internals -------------------------------------------------------------

    private static ListTag getList(Player player) {
        return player.getPersistentData().getList(DISABLED_EFFECT_IDS_KEY, Tag.TAG_STRING);
    }

    private static boolean containsId(Player player, ResourceLocation id) {
        return containsString(getList(player), id.toString());
    }

    private static boolean containsString(ListTag list, String s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getAsString().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /** Removes all crown/chestplate-managed effects right now so the HUD updates immediately. */
    private static void clearAllManagedEffects(Player player) {
        player.removeEffect(com.Momik.usless_mobs.registry.ModEffects.ELASTICITY.get());
        player.removeEffect(com.Momik.usless_mobs.registry.ModEffects.GOLDEN_FLOW.get());
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.ABSORPTION);
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DIG_SPEED);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.JUMP);
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.SATURATION);
    }
}
