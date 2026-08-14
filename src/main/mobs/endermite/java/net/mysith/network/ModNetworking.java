package net.mysith.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.mysith.MySithMod;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(MySithMod.MODID, "sith_main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.messageBuilder(DeathMarkSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DeathMarkSyncPacket::encode)
                .decoder(DeathMarkSyncPacket::decode)
                .consumerMainThread(DeathMarkSyncPacket::handle)
                .add();
    }

    public static void sendDeathMark(LivingEntity target, int durationTicks) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new DeathMarkSyncPacket(target.getId(), durationTicks)
        );
    }
}
