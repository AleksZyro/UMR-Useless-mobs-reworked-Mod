package net.mysith.silverfish;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CorruptionResonatorItem extends Item {
    private static final String TARGET_X = "CorruptionTargetX";
    private static final String TARGET_Y = "CorruptionTargetY";
    private static final String TARGET_Z = "CorruptionTargetZ";
    private static final String TARGET_HIDDEN = "CorruptionTargetHidden";
    private static final int ACTIVE_SCAN_INTERVAL = 20;
    private static final DustParticleOptions RESONATOR_DUST =
            new DustParticleOptions(new Vector3f(0.55F, 0.08F, 0.90F), 1.0F);

    public CorruptionResonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }

        boolean active = selected || player.getOffhandItem() == stack;
        if (!active) {
            return;
        }

        if (player.tickCount % ACTIVE_SCAN_INTERVAL == 0) {
            CorruptedSilverfishTracker.SearchResult result = CorruptedSilverfishTracker.findNearest(serverLevel, player, 36);
            writeTarget(stack, result);
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET_X)) {
            return;
        }

        BlockPos target = new BlockPos(tag.getInt(TARGET_X), tag.getInt(TARGET_Y), tag.getInt(TARGET_Z));
        double distance = Math.sqrt(target.distToCenterSqr(player.position()));
        int interval = CorruptedSilverfishTracker.vibrationInterval(distance);
        if (player.tickCount % interval != 0) {
            return;
        }

        float closeness = Mth.clamp(1.0F - ((float) distance / 36.0F), 0.05F, 1.0F);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.20F + closeness * 0.65F, 0.65F + closeness * 1.25F);
        serverLevel.sendParticles(RESONATOR_DUST,
                player.getX(), player.getY(0.75D), player.getZ(),
                2 + (int) (closeness * 6.0F), 0.25D, 0.18D, 0.25D, 0.0D);

        if (distance <= 7.0D && player.tickCount % (interval * 2) == 0) {
            CorruptedSilverfishTracker.reveal(serverLevel, target, false);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        CorruptedSilverfishTracker.SearchResult result = CorruptedSilverfishTracker.findNearest(serverLevel, player, CorruptedSilverfishTracker.DEFAULT_RESONATOR_RADIUS);
        writeTarget(stack, result);
        if (result == null) {
            player.displayClientMessage(Component.translatable("item.usless_mobs.corruption_resonator.none"), true);
        } else {
            String stateKey = result.hidden()
                    ? "item.usless_mobs.corruption_resonator.hidden"
                    : "item.usless_mobs.corruption_resonator.entity";
            player.displayClientMessage(Component.translatable(stateKey, (int) result.distance()), true);
            CorruptedSilverfishTracker.reveal(serverLevel, result.pos(), false);
        }

        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void writeTarget(ItemStack stack, @Nullable CorruptedSilverfishTracker.SearchResult result) {
        if (result == null) {
            stack.removeTagKey(TARGET_X);
            stack.removeTagKey(TARGET_Y);
            stack.removeTagKey(TARGET_Z);
            stack.removeTagKey(TARGET_HIDDEN);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TARGET_X, result.pos().getX());
        tag.putInt(TARGET_Y, result.pos().getY());
        tag.putInt(TARGET_Z, result.pos().getZ());
        tag.putBoolean(TARGET_HIDDEN, result.hidden());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(TARGET_X);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.corruption_resonator.tooltip.scan").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.usless_mobs.corruption_resonator.tooltip.vibrate").withStyle(ChatFormatting.GRAY));
    }
}
