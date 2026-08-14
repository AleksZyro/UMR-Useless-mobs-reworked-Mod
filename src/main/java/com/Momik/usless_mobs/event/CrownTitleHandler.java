package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.world.TrueCrownTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class CrownTitleHandler {
    private CrownTitleHandler() {
    }

    @SubscribeEvent
    public static void refreshTabTitle(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 40 == 0) {
            player.refreshTabListName();
        }
    }

    @SubscribeEvent
    public static void onTabListName(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        AllegiancePath path = pathFor(helmet);
        if (path == AllegiancePath.NONE) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null || !TrueCrownTracker.get(server).canClaimFinalPath(path, player.getUUID())) {
            return;
        }

        event.setDisplayName(Component.empty()
                .append(titleFor(path))
                .append(Component.literal(" "))
                .append(player.getName()));
    }

    private static AllegiancePath pathFor(ItemStack stack) {
        if (stack.is(com.Momik.usless_mobs.registry.ModItems.TRUE_VOID_HELMET.get())) {
            return AllegiancePath.VOID;
        }
        if (stack.is(com.Momik.usless_mobs.registry.ModItems.TRUE_LIVING_HELMET.get())) {
            return AllegiancePath.LIVING;
        }
        if (stack.is(com.Momik.usless_mobs.registry.ModItems.TRUE_CELESTIAL_HELMET.get())) {
            return AllegiancePath.CELESTIAL;
        }
        return AllegiancePath.NONE;
    }

    private static Component titleFor(AllegiancePath path) {
        if (path == AllegiancePath.VOID) {
            return Component.translatable("title.usless_mobs.true_void_helmet").withStyle(ChatFormatting.DARK_PURPLE);
        }
        if (path == AllegiancePath.LIVING) {
            return Component.translatable("title.usless_mobs.true_living_helmet").withStyle(ChatFormatting.GREEN);
        }
        if (path == AllegiancePath.CELESTIAL) {
            return Component.translatable("title.usless_mobs.true_celestial_helmet").withStyle(ChatFormatting.AQUA);
        }
        return Component.empty();
    }
}
