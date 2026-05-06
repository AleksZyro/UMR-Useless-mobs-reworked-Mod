package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SlimeReactorChestplateItem extends ArmorItem {

    public SlimeReactorChestplateItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.CHESTPLATE, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return Usless_mobs.MODID + ":textures/models/armor/schleimreaktor_layer_1.png";
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        player.fallDistance = Math.min(player.fallDistance, 1.5F);
        player.addEffect(new MobEffectInstance(Usless_mobs.ELASTICITY.get(), 40, 0, true, false, true));
        player.addEffect(new MobEffectInstance(Usless_mobs.GOLDEN_FLOW.get(), 40, 0, true, false, true));

        if (player.isOnFire() || player.isInLava()) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
        }

        if (!player.onGround() && player.isShiftKeyDown() && player.getDeltaMovement().y < 0.0D) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, Math.max(movement.y, -0.08D), movement.z);
            player.fallDistance = 0.0F;
        }

        if (player.onGround() && player.isSprinting() && player.tickCount % 10 == 0) {
            Vec3 lookDirection = player.getLookAngle();
            player.push(lookDirection.x * 0.12D, 0.05D, lookDirection.z * 0.12D);
            player.hasImpulse = true;
        }
    }
}
