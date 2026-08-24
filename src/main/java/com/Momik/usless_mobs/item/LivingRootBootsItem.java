package com.Momik.usless_mobs.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class LivingRootBootsItem extends ArmorItem {
    private static final String NEXT_ROOT_MEND_KEY = "UslessMobsLivingBootsNextRootMend";
    private static final long ROOT_MEND_COOLDOWN_TICKS = 20L * 18L;
    private static final Multimap<Attribute, AttributeModifier> ATTRIBUTES = createAttributes();

    public LivingRootBootsItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.BOOTS, properties);
    }

    private static Multimap<Attribute, AttributeModifier> createAttributes() {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        EquipmentSlot slot = EquipmentSlot.FEET;
        builder.put(Attributes.ARMOR,                UmrAttributes.addition("living_root_boots", slot, "armor",     "Living root boots armor",     5.0D));
        builder.put(Attributes.ARMOR_TOUGHNESS,      UmrAttributes.addition("living_root_boots", slot, "tough",     "Living root boots toughness", 4.0D));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, UmrAttributes.addition("living_root_boots", slot, "knockback", "Living root boots grip",      0.12D));
        builder.put(Attributes.MOVEMENT_SPEED,       UmrAttributes.multiplyTotal("living_root_boots", slot, "speed", "Living root boots speed",     0.06D));
        return builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET ? ATTRIBUTES : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
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
        var below = level.getBlockState(player.blockPosition().below());
        boolean natural = below.is(BlockTags.LEAVES) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.ROOTED_DIRT);
        if (natural) {
            SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 80, 0);
            SlimePowerToggle.applyFiltered(player, MobEffects.JUMP, 80, 0);
            if (!level.isClientSide && isWearingLivingHelmet(player) && player.getHealth() < player.getMaxHealth() * 0.75F) {
                long now = level.getGameTime();
                long nextMend = player.getPersistentData().getLong(NEXT_ROOT_MEND_KEY);
                if (now >= nextMend) {
                    SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 80, 0);
                    player.getPersistentData().putLong(NEXT_ROOT_MEND_KEY, now + ROOT_MEND_COOLDOWN_TICKS);
                }
            }
        }
        if (!player.onGround() && player.getDeltaMovement().y < -0.25D) {
            player.fallDistance = Math.min(player.fallDistance, 1.5F);
            SlimePowerToggle.applyFiltered(player, MobEffects.SLOW_FALLING, 20, 0);
        }
        if (player.getTicksFrozen() > 0) {
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - 4));
        }
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return Usless_mobs.MODID + ":textures/models/armor/living_crystal_layer_1.png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.living_root_boots.tooltip.stats").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.usless_mobs.living_root_boots.tooltip.motion").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.usless_mobs.living_root_boots.tooltip.set_bonus").withStyle(ChatFormatting.DARK_GREEN));
    }

    private static boolean isWearingLivingHelmet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(com.Momik.usless_mobs.registry.ModItems.LIVING_CRYSTAL_HELMET.get());
    }
}
