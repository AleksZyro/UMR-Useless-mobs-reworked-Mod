package com.Momik.usless_mobs.network;

import net.minecraftforge.eventbus.api.Event;

/** Common event boundary; only the client cue player subscribes to it. */
public final class FrostStrayImpactPacketEvent extends Event {
    private final FrostStrayImpactPacket packet;

    public FrostStrayImpactPacketEvent(FrostStrayImpactPacket packet) {
        this.packet = packet;
    }

    public FrostStrayImpactPacket packet() {
        return this.packet;
    }
}
