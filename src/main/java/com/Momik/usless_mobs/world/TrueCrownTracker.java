package com.Momik.usless_mobs.world;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class TrueCrownTracker extends SavedData {
    private static final String DATA_NAME = "usless_mobs_true_crown_tracker";
    private static final String CRAFTED_KEY = "Crafted";
    private static final String HOLDER_KEY = "Holder";
    private static final String HOLDER_NAME_KEY = "HolderName";
    private static final String VOID_HOLDER_KEY = "VoidHolder";
    private static final String CELESTIAL_HOLDER_KEY = "CelestialHolder";
    private static final String LIVING_HOLDER_KEY = "LivingHolder";
    private static final String VOID_HOLDER_NAME_KEY = "VoidHolderName";
    private static final String CELESTIAL_HOLDER_NAME_KEY = "CelestialHolderName";
    private static final String LIVING_HOLDER_NAME_KEY = "LivingHolderName";

    private boolean crafted;
    private UUID holder;
    private String holderName = "";
    private UUID voidHolder;
    private UUID celestialHolder;
    private UUID livingHolder;
    private String voidHolderName = "";
    private String celestialHolderName = "";
    private String livingHolderName = "";

    public static TrueCrownTracker get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TrueCrownTracker::load,
                TrueCrownTracker::new,
                DATA_NAME
        );
    }

    public static TrueCrownTracker load(CompoundTag tag) {
        TrueCrownTracker data = new TrueCrownTracker();
        data.crafted = tag.getBoolean(CRAFTED_KEY);
        if (tag.hasUUID(HOLDER_KEY)) {
            data.holder = tag.getUUID(HOLDER_KEY);
        }
        data.holderName = tag.getString(HOLDER_NAME_KEY);
        if (tag.hasUUID(VOID_HOLDER_KEY)) {
            data.voidHolder = tag.getUUID(VOID_HOLDER_KEY);
        }
        if (tag.hasUUID(CELESTIAL_HOLDER_KEY)) {
            data.celestialHolder = tag.getUUID(CELESTIAL_HOLDER_KEY);
        }
        if (tag.hasUUID(LIVING_HOLDER_KEY)) {
            data.livingHolder = tag.getUUID(LIVING_HOLDER_KEY);
        }
        data.voidHolderName = tag.getString(VOID_HOLDER_NAME_KEY);
        data.celestialHolderName = tag.getString(CELESTIAL_HOLDER_NAME_KEY);
        data.livingHolderName = tag.getString(LIVING_HOLDER_NAME_KEY);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(CRAFTED_KEY, crafted);
        if (holder != null) {
            tag.putUUID(HOLDER_KEY, holder);
        }
        tag.putString(HOLDER_NAME_KEY, holderName);
        if (voidHolder != null) {
            tag.putUUID(VOID_HOLDER_KEY, voidHolder);
        }
        if (celestialHolder != null) {
            tag.putUUID(CELESTIAL_HOLDER_KEY, celestialHolder);
        }
        if (livingHolder != null) {
            tag.putUUID(LIVING_HOLDER_KEY, livingHolder);
        }
        tag.putString(VOID_HOLDER_NAME_KEY, voidHolderName);
        tag.putString(CELESTIAL_HOLDER_NAME_KEY, celestialHolderName);
        tag.putString(LIVING_HOLDER_NAME_KEY, livingHolderName);
        return tag;
    }

    public boolean hasBeenCrafted() {
        return crafted;
    }

    public void markCrafted(UUID holder, String holderName) {
        this.crafted = true;
        this.holder = holder;
        this.holderName = holderName == null ? "" : holderName;
        setDirty();
    }

    public boolean isFinalPathClaimed(AllegiancePath path) {
        return holderFor(path) != null;
    }

    public boolean canClaimFinalPath(AllegiancePath path, UUID holder) {
        UUID current = holderFor(path);
        return current == null || current.equals(holder);
    }

    public boolean claimFinalPath(AllegiancePath path, UUID holder, String holderName) {
        if (path == AllegiancePath.NONE || holder == null || !canClaimFinalPath(path, holder)) {
            return false;
        }
        if (path == AllegiancePath.VOID) {
            this.voidHolder = holder;
            this.voidHolderName = holderName == null ? "" : holderName;
        } else if (path == AllegiancePath.CELESTIAL) {
            this.celestialHolder = holder;
            this.celestialHolderName = holderName == null ? "" : holderName;
        } else if (path == AllegiancePath.LIVING) {
            this.livingHolder = holder;
            this.livingHolderName = holderName == null ? "" : holderName;
        }
        setDirty();
        return true;
    }

    public int claimedFinalPathCount() {
        int count = 0;
        if (voidHolder != null) {
            count++;
        }
        if (celestialHolder != null) {
            count++;
        }
        if (livingHolder != null) {
            count++;
        }
        return count;
    }

    private UUID holderFor(AllegiancePath path) {
        if (path == AllegiancePath.VOID) {
            return voidHolder;
        }
        if (path == AllegiancePath.CELESTIAL) {
            return celestialHolder;
        }
        if (path == AllegiancePath.LIVING) {
            return livingHolder;
        }
        return null;
    }
}
