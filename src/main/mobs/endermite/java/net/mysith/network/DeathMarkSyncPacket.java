package net.mysith.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mysith.client.ClientDeathMarkSync;

public class DeathMarkSyncPacket {
    private final int entityId;
    private final int durationTicks;

    public DeathMarkSyncPacket(int entityId, int durationTicks) {
        this.entityId = entityId;
        this.durationTicks = durationTicks;
    }

    public static void encode(DeathMarkSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static DeathMarkSyncPacket decode(FriendlyByteBuf buffer) {
        return new DeathMarkSyncPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(DeathMarkSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDeathMarkSync.markEntity(packet.entityId, packet.durationTicks)
        ));
        context.setPacketHandled(true);
    }
}
