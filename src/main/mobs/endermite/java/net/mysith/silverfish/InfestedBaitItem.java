package net.mysith.silverfish;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;

public class InfestedBaitItem extends Item {
    public InfestedBaitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || !(context.getLevel() instanceof ServerLevel serverLevel) || context.getPlayer() == null) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (context.getPlayer().getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }

        BlockPos clicked = context.getClickedPos().relative(context.getClickedFace());
        CorruptedSilverfishTracker.SearchResult result = CorruptedSilverfishTracker.findNearest(serverLevel, context.getPlayer(), 16);
        if (result != null && result.hidden()) {
            CorruptedSilverfishTracker.spawnFromHost(serverLevel, result.pos(), context.getPlayer());
            context.getPlayer().displayClientMessage(Component.translatable("item.usless_mobs.infested_bait.lured"), true);
        } else if (clicked.getY() <= 48 && !serverLevel.canSeeSky(clicked) && serverLevel.getRandom().nextFloat() < 0.45F) {
            CorruptedSilverfishTracker.spawnFromHost(serverLevel, clicked, context.getPlayer());
            context.getPlayer().displayClientMessage(Component.translatable("item.usless_mobs.infested_bait.lured"), true);
        } else {
            serverLevel.playSound(null, clicked, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 0.75F, 0.55F);
            context.getPlayer().displayClientMessage(Component.translatable("item.usless_mobs.infested_bait.waiting"), true);
        }

        if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        context.getPlayer().getCooldowns().addCooldown(this, 20);
        return InteractionResult.SUCCESS;
    }
}
