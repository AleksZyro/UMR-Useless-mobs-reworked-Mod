package net.mysith.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.mysith.event.SoulEndermiteSpawnHandler;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TheEndGatewayBlockEntity.class)
public class EndGatewayBlockEntityMixin {

    @Inject(
            method = "teleportEntity",
            at = @At("HEAD")
    )
    private static void mysith$queueGatewayPearlSpawn(Level level, BlockPos pos, BlockState state,
                                                      Entity entity, TheEndGatewayBlockEntity gateway,
                                                      CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (gateway.isCoolingDown()) return;
        if (!(entity instanceof ThrownEnderpearl pearl)) return;

        Entity owner = pearl.getOwner();
        if (!(owner instanceof Player player)) {
            net.mysith.MySithMod.LOGGER.warn("[SoulEndermite] Gateway pearl had no player owner at {}", pos);
            return;
        }

        SoulEndermiteSpawnHandler.queueGatewayPearlTeleport(serverLevel, player, pearl.getUUID(), "gateway-mixin");
    }
}
