package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.network.ModNetwork;
import com.Momik.usless_mobs.network.ToggleSlimeEffectsPacket;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        while (ModKeyMappings.TOGGLE_SLIME_EFFECTS.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new ToggleSlimeEffectsPacket());
        }
    }
}
