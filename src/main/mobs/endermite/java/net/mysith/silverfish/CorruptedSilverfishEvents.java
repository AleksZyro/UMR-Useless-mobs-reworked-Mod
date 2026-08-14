package net.mysith.silverfish;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class CorruptedSilverfishEvents {
    private CorruptedSilverfishEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (!CorruptedSilverfishTracker.isHostBlock(state) || !CorruptedSilverfishTracker.isCorruptedHost(level, pos)) {
            return;
        }

        CorruptedSilverfishEntity silverfish = CorruptedSilverfishTracker.spawnFromHost(level, pos, player);
        if (silverfish != null && player.getItemBySlot(EquipmentSlot.LEGS).is(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_CRYSTAL_LEGGINGS.get())) {
            silverfish.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 120, 0));
        }
    }

    public static boolean isWearingCorruptedLeggings(net.minecraft.world.entity.LivingEntity entity) {
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        return legs.is(com.Momik.usless_mobs.registry.ModItems.CORRUPTED_CRYSTAL_LEGGINGS.get());
    }
}
