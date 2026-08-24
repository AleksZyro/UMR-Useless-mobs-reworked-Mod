package com.Momik.usless_mobs.compat.curios;

import com.Momik.usless_mobs.effect.KingSlimeCrownEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class KingSlimeCrownCurio implements ICurio {

    private final ItemStack stack;

    public KingSlimeCrownCurio(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        if (slotContext.entity() instanceof Player player) {
            // Pick the right tier based on which crown variant is in the slot.
            KingSlimeCrownEffects.Tier tier = stack.getItem() == com.Momik.usless_mobs.registry.ModItems.NETHERITE_KINGS_KRONE.get()
                    ? KingSlimeCrownEffects.NETHERITE
                    : KingSlimeCrownEffects.STANDARD;
            KingSlimeCrownEffects.apply(player, tier);
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) {
        return true;
    }

    @Override
    public DropRule getDropRule(SlotContext slotContext, net.minecraft.world.damagesource.DamageSource source,
                                int lootingLevel, boolean recentlyHit) {
        return DropRule.DEFAULT;
    }
}
