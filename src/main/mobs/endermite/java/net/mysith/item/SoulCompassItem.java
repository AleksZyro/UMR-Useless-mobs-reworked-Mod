package net.mysith.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.mysith.world.ScytheTracker;

public class SoulCompassItem extends Item {

    public static final String TARGET_X = "TargetX";
    public static final String TARGET_Y = "TargetY";
    public static final String TARGET_Z = "TargetZ";
    public static final String TARGET_DIM = "TargetDim";
    public static final String HAS_TARGET = "HasTarget";

    public SoulCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(HAS_TARGET);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 10 != 0) return; // alle 0.5s

        ScytheTracker tracker = ScytheTracker.get(sl);
        CompoundTag tag = stack.getOrCreateTag();
        UUID holderUuid = tracker.getHolderUuid();

        if (holderUuid == null) {
            tag.putBoolean(HAS_TARGET, false);
            return;
        }

        tag.putDouble(TARGET_X, tracker.getHolderX());
        tag.putDouble(TARGET_Y, tracker.getHolderY());
        tag.putDouble(TARGET_Z, tracker.getHolderZ());
        tag.putString(TARGET_DIM, tracker.getHolderDimension());
        tag.putBoolean(HAS_TARGET, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.success(stack);
        }

        ScytheTracker tracker = ScytheTracker.get(sl);
        if (tracker.getHolderUuid() == null || tracker.getGeneration() == 0) {
            player.sendSystemMessage(Component.translatable("item.usless_mobs.soul_compass.no_target")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.success(stack);
        }

        String name = tracker.getHolderName();
        Component dim = formatDim(tracker.getHolderDimension());
        String playerDim = player.level().dimension().location().toString();

        player.sendSystemMessage(
                Component.translatable("item.usless_mobs.soul_compass.wielded_by",
                        Component.literal(name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .withStyle(ChatFormatting.DARK_RED)
        );
        player.sendSystemMessage(
                Component.translatable("item.usless_mobs.soul_compass.dimension",
                        dim.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC))
                        .withStyle(ChatFormatting.GRAY)
        );

        if (tracker.getHolderDimension().equals(playerDim)) {
            double dx = tracker.getHolderX() - player.getX();
            double dz = tracker.getHolderZ() - player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            player.sendSystemMessage(
                    Component.translatable("item.usless_mobs.soul_compass.distance",
                            Component.literal(Integer.toString(dist)).withStyle(ChatFormatting.RED))
                            .withStyle(ChatFormatting.GRAY)
            );
        } else {
            player.sendSystemMessage(
                    Component.translatable("item.usless_mobs.soul_compass.wrong_realm")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
            );
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.soul_compass.tooltip1")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.soul_compass.tooltip2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component formatDim(String dim) {
        if (dim == null || dim.isEmpty()) return Component.translatable("item.usless_mobs.soul_compass.dimension.unknown");
        return switch (dim) {
            case "minecraft:overworld" -> Component.translatable("item.usless_mobs.soul_compass.dimension.overworld");
            case "minecraft:the_nether" -> Component.translatable("item.usless_mobs.soul_compass.dimension.nether");
            case "minecraft:the_end" -> Component.translatable("item.usless_mobs.soul_compass.dimension.end");
            default -> Component.literal(dim);
        };
    }
}
