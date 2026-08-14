package net.mysith.world;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent server-wide leaderboard. Per-player kill totals + best streaks. */
public class ReaperStats extends SavedData {

    private static final String DATA_NAME = "mysith_reaper_stats";

    public static class Entry {
        public UUID uuid;
        public String name;
        public int kills;
        public int bestStreak;
        public int deathsToScythe;

        public Entry(UUID u, String n) {
            this.uuid = u;
            this.name = n;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static ReaperStats get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(ReaperStats::load, ReaperStats::new, DATA_NAME);
    }

    public static ReaperStats load(CompoundTag tag) {
        ReaperStats s = new ReaperStats();
        ListTag list = tag.getList("Entries", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            UUID u = e.getUUID("UUID");
            Entry entry = new Entry(u, e.getString("Name"));
            entry.kills = e.getInt("Kills");
            entry.bestStreak = e.getInt("BestStreak");
            entry.deathsToScythe = e.getInt("DeathsToScythe");
            s.entries.put(u, entry);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("UUID", e.uuid);
            c.putString("Name", e.name);
            c.putInt("Kills", e.kills);
            c.putInt("BestStreak", e.bestStreak);
            c.putInt("DeathsToScythe", e.deathsToScythe);
            list.add(c);
        }
        tag.put("Entries", list);
        return tag;
    }

    public Entry getOrCreate(UUID uuid, String name) {
        Entry e = entries.computeIfAbsent(uuid, u -> new Entry(u, name));
        e.name = name;
        setDirty();
        return e;
    }

    public List<Entry> getTopByKills(int n) {
        List<Entry> sorted = new ArrayList<>(entries.values());
        sorted.sort((a, b) -> Integer.compare(b.kills, a.kills));
        return sorted.subList(0, Math.min(n, sorted.size()));
    }
}
