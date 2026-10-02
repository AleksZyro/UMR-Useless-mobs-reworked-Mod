package com.Momik.usless_mobs.network;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

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
        CHANNEL.messageBuilder(FrostStrayAbilityPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FrostStrayAbilityPacket::encode)
                .decoder(FrostStrayAbilityPacket::decode)
                .consumerMainThread(FrostStrayAbilityPacket::handle)
                .add();
        CHANNEL.messageBuilder(FrostStrayImpactPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FrostStrayImpactPacket::encode)
                .decoder(FrostStrayImpactPacket::decode)
                .consumerMainThread(FrostStrayImpactPacket::handle)
                .add();
    }

    public static void sendToTrackingAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
