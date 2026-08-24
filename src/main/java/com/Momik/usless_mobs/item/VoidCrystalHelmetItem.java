package com.Momik.usless_mobs.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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
import org.jetbrains.annotations.Nullable;

public class VoidCrystalHelmetItem extends ArmorItem {
    private static final String NEXT_VOID_GUARD_KEY = "UslessMobsVoidHelmetNextGuard";
    private static final Multimap<Attribute, AttributeModifier> ATTRIBUTES = createAttributes();

    public VoidCrystalHelmetItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.HELMET, properties);
    }

    private static Multimap<Attribute, AttributeModifier> createAttributes() {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        EquipmentSlot slot = EquipmentSlot.HEAD;
        builder.put(Attributes.ARMOR,                UmrAttributes.addition("void_crystal_helmet", slot, "armor",     "Void crystal helmet armor",     7.0D));
        builder.put(Attributes.ARMOR_TOUGHNESS,      UmrAttributes.addition("void_crystal_helmet", slot, "tough",     "Void crystal helmet toughness", 5.0D));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, UmrAttributes.addition("void_crystal_helmet", slot, "knockback", "Void crystal helmet anchor",    0.18D));
        builder.put(Attributes.MAX_HEALTH,           UmrAttributes.addition("void_crystal_helmet", slot, "health",    "Void crystal helmet vitality",  6.0D));
        return builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? ATTRIBUTES : super.getDefaultAttributeModifiers(slot);
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
        if (level.getMaxLocalRawBrightness(player.blockPosition()) < 8) {
            SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
        }
        if (!level.isClientSide && player.getHealth() <= player.getMaxHealth() * 0.35F) {
            long now = level.getGameTime();
            long nextGuard = player.getPersistentData().getLong(NEXT_VOID_GUARD_KEY);
            if (now >= nextGuard) {
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 200, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 100, 0);
                player.getPersistentData().putLong(NEXT_VOID_GUARD_KEY, now + 20L * 28L);
            }
        }
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return Usless_mobs.MODID + ":textures/models/armor/living_crystal_layer_1.png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.void_crystal_helmet.tooltip.stats").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.usless_mobs.void_crystal_helmet.tooltip.guard").withStyle(ChatFormatting.GRAY));
    }
}
