package com.Momik.usless_mobs.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

/** Timeline control messages are server-to-client only; clients never request an attack. */
public record FrostStrayAbilityPacket(Type type, int entityId, UUID instanceId,
                                      long startGameTime, long serverGameTime, int durationTicks) {
    public enum Type {
        START,
        RELEASE,
        CANCEL
    }

    public FrostStrayAbilityPacket {
        if (type == null || entityId <= 0 || instanceId == null || startGameTime < 0L
                || serverGameTime < startGameTime || durationTicks <= 0) {
            throw new IllegalArgumentException("invalid Frost Stray ability packet");
        }
    }

    public static void encode(FrostStrayAbilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.type.ordinal());
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.instanceId);
        buffer.writeLong(packet.startGameTime);
        buffer.writeLong(packet.serverGameTime);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static FrostStrayAbilityPacket decode(FriendlyByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        Type[] types = Type.values();
        if (ordinal >= types.length) {
            throw new IllegalArgumentException("unknown Frost Stray ability packet type");
        }
        return new FrostStrayAbilityPacket(types[ordinal], buffer.readVarInt(), buffer.readUUID(),
                buffer.readLong(), buffer.readLong(), buffer.readVarInt());
    }

    public static void handle(FrostStrayAbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new FrostStrayAbilityPacketEvent(packet)));
        context.setPacketHandled(true);
    }
}
