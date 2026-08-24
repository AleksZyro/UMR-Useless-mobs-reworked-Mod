package com.Momik.usless_mobs.allegiance;

public enum AllegianceStage {
    NONE(0),
    INITIATED(1),
    PATH_CHOSEN(2),
    PATH_TRIAL(3),
    ASCENDED(4),
    MASTERED(5);

    public final int level;

    AllegianceStage(int level) {
        this.level = level;
    }

    public boolean isAtLeast(AllegianceStage other) {
        return this.level >= other.level;
    }
}
