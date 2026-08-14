package net.mysith.event;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public class AwakenedCrystalHandler {
    private static final String BLOOD_KILLS_KEY = "MysithVoidCrystalBloodKills";
    private static final int BLOOD_KILLS_REQUIRED = 25;

    private AwakenedCrystalHandler() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (!(dead instanceof Monster)) {
            return;
        }

        ItemStack crystal = findVoidCrystal(player);
        if (crystal.isEmpty()) {
            return;
        }

        int kills = crystal.getOrCreateTag().getInt(BLOOD_KILLS_KEY) + 1;
        if (kills < BLOOD_KILLS_REQUIRED) {
            crystal.getOrCreateTag().putInt(BLOOD_KILLS_KEY, kills);
            if (kills % 5 == 0) {
                player.displayClientMessage(Component.translatable("item.usless_mobs.void_crystal.blood_progress", kills, BLOOD_KILLS_REQUIRED)
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
            return;
        }

        crystal.shrink(1);
        if (!crystal.isEmpty() && crystal.hasTag()) {
            crystal.getTag().remove(BLOOD_KILLS_KEY);
        }
        ItemStack awakened = new ItemStack(ModItems.AWAKENED_VOID_CRYSTAL.get());
        if (!player.getInventory().add(awakened)) {
            player.drop(awakened, false);
        }
        player.displayClientMessage(Component.translatable("item.usless_mobs.awakened_void_crystal.created")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    private static ItemStack findVoidCrystal(Player player) {
        if (player.getMainHandItem().is(ModItems.VOID_CRYSTAL.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModItems.VOID_CRYSTAL.get())) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }
}
