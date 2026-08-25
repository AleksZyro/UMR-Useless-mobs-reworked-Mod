package com.Momik.usless_mobs.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class WhaleRuinEncounterData extends SavedData {
    private static final String DATA_NAME = "usless_mobs_whale_ruin_encounters";
    private final Map<String, Encounter> encounters = new HashMap<>();

    public static WhaleRuinEncounterData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                WhaleRuinEncounterData::load,
                WhaleRuinEncounterData::new,
                DATA_NAME);
    }

    public static WhaleRuinEncounterData load(CompoundTag tag) {
        WhaleRuinEncounterData data = new WhaleRuinEncounterData();
        ListTag list = tag.getList("Encounters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag saved = list.getCompound(i);
            String key = saved.getString("Key");
            if (key.isEmpty()) {
                continue;
            }
            UUID bossUuid = saved.hasUUID("Boss") ? saved.getUUID("Boss") : null;
            data.encounters.put(key, new Encounter(
                    saved.getBoolean("Active"),
                    saved.getBoolean("Defeated"),
                    bossUuid,
                    saved.getLong("Origin")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        encounters.forEach((key, encounter) -> {
            CompoundTag saved = new CompoundTag();
            saved.putString("Key", key);
            saved.putBoolean("Active", encounter.active());
            saved.putBoolean("Defeated", encounter.defeated());
            saved.putLong("Origin", encounter.origin());
            if (encounter.bossUuid() != null) {
                saved.putUUID("Boss", encounter.bossUuid());
            }
            list.add(saved);
        });
        tag.put("Encounters", list);
        return tag;
    }

    public boolean activateIfInactive(String key, long origin) {
        Encounter old = encounters.get(key);
        if (old != null && (old.active() || old.defeated())) {
            return false;
        }
        encounters.put(key, new Encounter(true, false, null, origin));
        setDirty();
        return true;
    }

    public void setBossUuid(String key, UUID bossUuid) {
        Encounter old = encounters.get(key);
        if (old == null || old.defeated()) {
            return;
        }
        encounters.put(key, new Encounter(true, false, bossUuid, old.origin()));
        setDirty();
    }

    public void markDefeated(String key) {
        Encounter old = encounters.get(key);
        if (old == null) {
            return;
        }
        encounters.put(key, new Encounter(false, true, null, old.origin()));
        setDirty();
    }

    @Nullable
    public Encounter getEncounter(String key) {
        return encounters.get(key);
    }

    public record Encounter(boolean active, boolean defeated, @Nullable UUID bossUuid, long origin) {
    }
}
