package com.Momik.usless_mobs.network;

import com.Momik.usless_mobs.effect.SlimePowerToggle;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ToggleSlimeEffectsPacket {
    public static void encode(ToggleSlimeEffectsPacket packet, FriendlyByteBuf buffer) {
    }

    public static ToggleSlimeEffectsPacket decode(FriendlyByteBuf buffer) {
        return new ToggleSlimeEffectsPacket();
    }

    public static void handle(ToggleSlimeEffectsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            SlimePowerToggle.toggle(player);
        }
        context.setPacketHandled(true);
    }
}
