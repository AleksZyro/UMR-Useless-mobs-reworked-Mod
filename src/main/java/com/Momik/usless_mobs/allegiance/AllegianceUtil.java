package com.Momik.usless_mobs.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AllegianceUtil {

    private static final String ALLEGIANCE_KEY = "UslessMobs_FinalAllegiance";
    private static final String STAGE_KEY      = "UslessMobs_AllegianceStage";
    private static final String PATH_KILLS_KEY = "UslessMobs_PathKillCount";
    private static final String SET_KILLS_KEY  = "UslessMobs_SetKillCount";

    private AllegianceUtil() {}

    // ── Path ──────────────────────────────────────────────────────────────────

    public static AllegiancePath getPath(Player player) {
        String value = player.getPersistentData().getString(ALLEGIANCE_KEY);
        if (value == null || value.isBlank()) return AllegiancePath.NONE;
        try { return AllegiancePath.valueOf(value); }
        catch (IllegalArgumentException ignored) { return AllegiancePath.NONE; }
    }

    public static boolean hasPath(Player player, AllegiancePath path) {
        return getPath(player) == path;
    }

    public static boolean hasAnyPath(Player player) {
        return getPath(player) != AllegiancePath.NONE;
    }

    public static boolean setIfUnset(ServerPlayer player, AllegiancePath path) {
        AllegiancePath current = getPath(player);
        if (current == AllegiancePath.NONE) {
            player.getPersistentData().putString(ALLEGIANCE_KEY, path.name());
            return true;
        }
        return current == path;
    }

    // ── Stage ─────────────────────────────────────────────────────────────────

    public static AllegianceStage getStage(Player player) {
        String val = player.getPersistentData().getString(STAGE_KEY);
        if (val.isBlank()) return AllegianceStage.NONE;
        try { return AllegianceStage.valueOf(val); }
        catch (IllegalArgumentException ignored) { return AllegianceStage.NONE; }
    }

    public static void setStage(Player player, AllegianceStage stage) {
        player.getPersistentData().putString(STAGE_KEY, stage.name());
    }

    public static boolean tryAdvanceStage(Player player, AllegianceStage from, AllegianceStage to) {
        if (getStage(player) == from) {
            setStage(player, to);
            return true;
        }
        return false;
    }

    // ── Kill counters ─────────────────────────────────────────────────────────

    public static int getPathKills(Player player) {
        return player.getPersistentData().getInt(PATH_KILLS_KEY);
    }

    public static int incrementPathKills(Player player) {
        int next = getPathKills(player) + 1;
        player.getPersistentData().putInt(PATH_KILLS_KEY, next);
        return next;
    }

    public static void resetPathKills(Player player) {
        player.getPersistentData().remove(PATH_KILLS_KEY);
    }

    public static int getSetKills(Player player) {
        return player.getPersistentData().getInt(SET_KILLS_KEY);
    }

    public static int incrementSetKills(Player player) {
        int next = getSetKills(player) + 1;
        player.getPersistentData().putInt(SET_KILLS_KEY, next);
        return next;
    }
}
