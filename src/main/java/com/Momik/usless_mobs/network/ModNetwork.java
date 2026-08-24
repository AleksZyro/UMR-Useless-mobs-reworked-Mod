package com.Momik.usless_mobs.network;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(Usless_mobs.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ToggleSlimeEffectsPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleSlimeEffectsPacket::encode)
                .decoder(ToggleSlimeEffectsPacket::decode)
                .consumerMainThread(ToggleSlimeEffectsPacket::handle)
                .add();
    }
}
