package com.Momik.usless_mobs.network;

import com.Momik.usless_mobs.client.FrostStrayAbilityClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** A server-confirmed projectile collision, carrying its actual collision position. */
public record FrostStrayImpactPacket(int entityId, UUID instanceId, UUID impactId, double x, double y, double z,
                                     long serverGameTime) {
    public FrostStrayImpactPacket {
        if (entityId <= 0 || instanceId == null || impactId == null || serverGameTime < 0L
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("invalid Frost Stray impact packet");
        }
    }

    public static void encode(FrostStrayImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.instanceId);
        buffer.writeUUID(packet.impactId);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeLong(packet.serverGameTime);
    }

    public static FrostStrayImpactPacket decode(FriendlyByteBuf buffer) {
        return new FrostStrayImpactPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readUUID(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readLong());
    }

    public static void handle(FrostStrayImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FrostStrayAbilityClient.handleImpact(packet)));
        context.setPacketHandled(true);
    }
}
