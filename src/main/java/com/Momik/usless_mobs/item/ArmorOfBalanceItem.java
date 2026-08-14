package com.Momik.usless_mobs.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.Momik.usless_mobs.client.WornTruePathArmorModel;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public class ArmorOfBalanceItem extends ArmorItem {
    private final Multimap<Attribute, AttributeModifier> attributes;

    public ArmorOfBalanceItem(Type type, Properties properties) {
        super(ArmorMaterials.NETHERITE, type, properties);
        this.attributes = createAttributes(type);
    }

    private static Multimap<Attribute, AttributeModifier> createAttributes(Type type) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        EquipmentSlot slot = type.getSlot();
        builder.put(Attributes.ARMOR,                UmrAttributes.addition("balance", slot, "armor",     "Armor of Balance armor",     armorValue(type)));
        builder.put(Attributes.ARMOR_TOUGHNESS,      UmrAttributes.addition("balance", slot, "tough",     "Armor of Balance toughness", toughnessValue(type)));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, UmrAttributes.addition("balance", slot, "knockback", "Armor of Balance poise",     knockbackValue(type)));
        if (type == Type.HELMET || type == Type.CHESTPLATE) {
            builder.put(Attributes.MAX_HEALTH, UmrAttributes.addition("balance", slot, "health", "Armor of Balance vitality", type == Type.CHESTPLATE ? 8.0D : 4.0D));
        }
        if (type == Type.BOOTS) {
            builder.put(Attributes.MOVEMENT_SPEED, UmrAttributes.multiplyTotal("balance", slot, "speed", "Armor of Balance stride", 0.05D));
        }
        return builder.build();
    }

    private static double armorValue(Type type) {
        return switch (type) {
            case HELMET -> 8.0D;
            case CHESTPLATE -> 13.0D;
            case LEGGINGS -> 11.0D;
            case BOOTS -> 6.0D;
        };
    }

    private static double toughnessValue(Type type) {
        return type == Type.CHESTPLATE ? 6.0D : 5.0D;
    }

    private static double knockbackValue(Type type) {
        return type == Type.CHESTPLATE ? 0.22D : 0.16D;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == getType().getSlot() ? attributes : super.getDefaultAttributeModifiers(slot);
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
        if (hasFullBalanceSet(player)) {
            SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 80, 0);
            SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 80, 1);
            SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 80, 0);
            if (!level.isClientSide && player.tickCount % 120 == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(2.0F);
            }
        }
    }

    private static boolean hasFullBalanceSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(com.Momik.usless_mobs.registry.ModItems.ARMOR_OF_BALANCE_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(com.Momik.usless_mobs.registry.ModItems.ARMOR_OF_BALANCE_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(com.Momik.usless_mobs.registry.ModItems.ARMOR_OF_BALANCE_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(com.Momik.usless_mobs.registry.ModItems.ARMOR_OF_BALANCE_BOOTS.get());
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String layer = getType() == Type.LEGGINGS ? "_layer_2.png" : "_layer_1.png";
        return Usless_mobs.MODID + ":textures/models/armor/true_balance" + layer;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HumanoidModel<?> model;

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot != getType().getSlot()) {
                    return original;
                }
                if (model == null) {
                    model = WornTruePathArmorModel.createBalanced(getType());
                }
                ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);
                WornTruePathArmorModel.showForType(model, getType());
                return model;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.armor_of_balance.tooltip.stats").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.usless_mobs.armor_of_balance.tooltip.set_bonus").withStyle(ChatFormatting.AQUA));
    }
}
