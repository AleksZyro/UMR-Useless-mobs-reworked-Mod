package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Trophy crown for the three path endings. Worn in the Curios "crown" slot
 * (not the armor head slot — that's the True path armor). Grants the path
 * aura + guard while equipped.
 */
public class PathCrownItem extends Item {
    public enum Path {
        VOID("item.usless_mobs.crown.void.tooltip", ChatFormatting.DARK_PURPLE, AllegiancePath.VOID),
        CELESTIAL("item.usless_mobs.crown.celestial.tooltip", ChatFormatting.AQUA, AllegiancePath.CELESTIAL),
        LIVING("item.usless_mobs.crown.living.tooltip", ChatFormatting.GREEN, AllegiancePath.LIVING);

        public final String tooltipKey;
        public final ChatFormatting color;
        public final AllegiancePath allegiance;

        Path(String tooltipKey, ChatFormatting color, AllegiancePath allegiance) {
            this.tooltipKey = tooltipKey;
            this.color = color;
            this.allegiance = allegiance;
        }
    }

    private final Path path;

    public PathCrownItem(Path path, Properties properties) {
        super(properties);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(path.tooltipKey).withStyle(path.color));
        tooltip.add(Component.translatable("item.usless_mobs.crown.curio_slot").withStyle(ChatFormatting.GOLD));
    }
}
