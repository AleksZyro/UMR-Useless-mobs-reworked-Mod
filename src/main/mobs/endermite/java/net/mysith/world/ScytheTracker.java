package net.mysith.world;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public class ScytheTracker extends SavedData {

    private static final String DATA_NAME = "mysith_scythe_tracker";

    private int generation = 0;
    private UUID holderUuid;
    private String holderName = "";
    private String holderDimension = "";
    private double holderX = 0, holderY = 0, holderZ = 0;

    public static ScytheTracker get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(ScytheTracker::load, ScytheTracker::new, DATA_NAME);
    }

    public static ScytheTracker load(CompoundTag tag) {
        ScytheTracker t = new ScytheTracker();
        t.generation = tag.getInt("Gen");
        if (tag.hasUUID("Holder")) t.holderUuid = tag.getUUID("Holder");
        t.holderName = tag.getString("HolderName");
        t.holderDimension = tag.getString("HolderDim");
        t.holderX = tag.getDouble("HolderX");
        t.holderY = tag.getDouble("HolderY");
        t.holderZ = tag.getDouble("HolderZ");
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Gen", generation);
        if (holderUuid != null) tag.putUUID("Holder", holderUuid);
        tag.putString("HolderName", holderName);
        tag.putString("HolderDim", holderDimension);
        tag.putDouble("HolderX", holderX);
        tag.putDouble("HolderY", holderY);
        tag.putDouble("HolderZ", holderZ);
        return tag;
    }

    public int getGeneration() { return generation; }

    public UUID getHolderUuid() { return holderUuid; }
    public String getHolderName() { return holderName; }
    public String getHolderDimension() { return holderDimension; }
    public double getHolderX() { return holderX; }
    public double getHolderY() { return holderY; }
    public double getHolderZ() { return holderZ; }

    public void newScytheCrafted(ServerPlayer player) {
        generation++;
        updateHolder(player);
    }

    public void updateHolder(ServerPlayer player) {
        holderUuid = player.getUUID();
        holderName = player.getName().getString();
        holderDimension = player.level().dimension().location().toString();
        holderX = player.getX();
        holderY = player.getY();
        holderZ = player.getZ();
        setDirty();
    }

    public void clearHolder() {
        holderUuid = null;
        holderName = "";
        holderDimension = "";
        setDirty();
    }
}
