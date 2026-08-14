package com.Momik.usless_mobs.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.Momik.usless_mobs.effect.KingSlimeCrownEffects;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SlimeReactorChestplateItem extends ArmorItem {
    private static final double REACTOR_ARMOR = 11.0D;
    private static final double REACTOR_TOUGHNESS = 4.5D;
    private static final double REACTOR_KNOCKBACK_RESISTANCE = 0.25D;
    private static final Multimap<Attribute, AttributeModifier> REACTOR_ATTRIBUTES = createReactorAttributes();

    public SlimeReactorChestplateItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.CHESTPLATE, properties);
    }

    private static Multimap<Attribute, AttributeModifier> createReactorAttributes() {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        EquipmentSlot slot = EquipmentSlot.CHEST;
        builder.put(Attributes.ARMOR,                UmrAttributes.addition("slime_reactor", slot, "armor",     "Slime reactor armor",               REACTOR_ARMOR));
        builder.put(Attributes.ARMOR_TOUGHNESS,      UmrAttributes.addition("slime_reactor", slot, "tough",     "Slime reactor toughness",           REACTOR_TOUGHNESS));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, UmrAttributes.addition("slime_reactor", slot, "knockback", "Slime reactor knockback resistance", REACTOR_KNOCKBACK_RESISTANCE));
        return builder.build();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? REACTOR_ATTRIBUTES : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return Usless_mobs.MODID + ":textures/models/armor/schleimreaktor_layer_1.png";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player) || player.getItemBySlot(getType().getSlot()) != stack) {
            return;
        }
        if (SlimePowerToggle.areEffectsDisabled(player)) {
            return;
        }

        player.fallDistance = Math.min(player.fallDistance, 1.0F);
        SlimePowerToggle.applyFiltered(player, com.Momik.usless_mobs.registry.ModEffects.ELASTICITY.get(), 60, 0);
        SlimePowerToggle.applyFiltered(player, com.Momik.usless_mobs.registry.ModEffects.GOLDEN_FLOW.get(), 60, 0);

        if (player.isOnFire() || player.isInLava()) {
            SlimePowerToggle.applyFiltered(player, MobEffects.FIRE_RESISTANCE, 100, 0);
        }

        if (!level.isClientSide && player.tickCount % 160 == 0 && player.getHealth() <= player.getMaxHealth() * 0.4F) {
            SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 120, 0);
            SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 60, 0);
        }

        if (hasRoyalReactorSetBonus(player)) {
            SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 80, 0);
            SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 80, 0);
            if (!level.isClientSide && player.tickCount % 120 == 0) {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_BOOST, 80, 0);
            }
            if (!level.isClientSide && player.tickCount % 100 == 0 && player.getHealth() < player.getMaxHealth() * 0.75F) {
                player.heal(0.5F);
            }
        }

        if (!player.onGround() && player.isShiftKeyDown() && player.getDeltaMovement().y < 0.0D) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, Math.max(movement.y, -0.12D), movement.z);
            player.fallDistance = 0.0F;
        }

        if (player.onGround() && player.isSprinting() && player.tickCount % 14 == 0) {
            Vec3 lookDirection = player.getLookAngle();
            player.push(lookDirection.x * 0.10D, 0.035D, lookDirection.z * 0.10D);
            player.hasImpulse = true;
        }
    }

    private static boolean hasRoyalReactorSetBonus(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get())
                || head.is(com.Momik.usless_mobs.registry.ModItems.NETHERITE_KINGS_KRONE.get())
                || KingSlimeCrownEffects.isCrownActive(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.schleimreaktor_brustpanzer.tooltip.stats").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.usless_mobs.schleimreaktor_brustpanzer.tooltip.mobility").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.usless_mobs.schleimreaktor_brustpanzer.tooltip.guard").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.usless_mobs.schleimreaktor_brustpanzer.tooltip.set_bonus").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
