package com.Momik.usless_mobs.network;

import net.minecraftforge.eventbus.api.Event;

/** Common event boundary; only the client cue player subscribes to it. */
public final class FrostStrayAbilityPacketEvent extends Event {
    private final FrostStrayAbilityPacket packet;

    public FrostStrayAbilityPacketEvent(FrostStrayAbilityPacket packet) {
        this.packet = packet;
    }

    public FrostStrayAbilityPacket packet() {
        return this.packet;
    }
}
