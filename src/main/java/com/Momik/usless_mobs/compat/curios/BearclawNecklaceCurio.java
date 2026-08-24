package com.Momik.usless_mobs.compat.curios;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class BearclawNecklaceCurio implements ICurio {
    private static final String NEXT_BERSERK_KEY = "UslessMobs_Bearclaw_NextBerserk";

    private final ItemStack stack;

    public BearclawNecklaceCurio(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide) {
            boolean awakened = this.stack.is(com.Momik.usless_mobs.registry.ModItems.AWAKENED_BEARCLAW_NECKLACE.get());
            float threshold = awakened ? 0.40F : 0.30F;
            if (player.getHealth() > player.getMaxHealth() * threshold) {
                return;
            }

            int now = player.tickCount;
            int next = player.getPersistentData().getInt(NEXT_BERSERK_KEY);
            if (next > now) {
                return;
            }

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, awakened ? 220 : 160, awakened ? 1 : 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, awakened ? 220 : 160, awakened ? 1 : 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, awakened ? 220 : 160, 0, true, false, true));
            player.getPersistentData().putInt(NEXT_BERSERK_KEY, now + (awakened ? 650 : 900));
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) {
        return true;
    }
}
