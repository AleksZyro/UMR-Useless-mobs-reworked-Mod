package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.client.WornTruePathArmorModel;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
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

public class TrueCrownItem extends ArmorItem {
    private static final String NEXT_GUARD_KEY = "UslessMobs_TrueCrown_NextGuard";
    private final Path path;

    public enum Path {
        BALANCED("item.usless_mobs.true_crown.tooltip.path", "item.usless_mobs.true_crown.tooltip.guard", ChatFormatting.LIGHT_PURPLE);

        private final String pathTooltipKey;
        private final String guardTooltipKey;
        private final ChatFormatting color;

        Path(String pathTooltipKey, String guardTooltipKey, ChatFormatting color) {
            this.pathTooltipKey = pathTooltipKey;
            this.guardTooltipKey = guardTooltipKey;
            this.color = color;
        }
    }

    public TrueCrownItem(Properties properties) {
        this(Path.BALANCED, properties);
    }

    public TrueCrownItem(Path path, Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.HELMET, properties);
        this.path = path;
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

        applyPathAura(player);
        sendAuraParticles(level, player);

        if (!level.isClientSide && player.tickCount % 60 == 0) {
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.WITHER);
        }

        int now = player.tickCount;
        int nextGuard = player.getPersistentData().getInt(NEXT_GUARD_KEY + "_" + path.name());
        if (!level.isClientSide && player.getHealth() <= player.getMaxHealth() * 0.35F && nextGuard <= now) {
            applyPathGuard(player);
            player.getPersistentData().putInt(NEXT_GUARD_KEY + "_" + path.name(), now + 1200);
        }
    }

    private void applyPathAura(Player player) {
        SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 80, 1);
        SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 80, 1);
        SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 80, 1);
        SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
    }

    private void applyPathGuard(Player player) {
        SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 260, 2);
        SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 140, 1);
        SlimePowerToggle.applyFiltered(player, MobEffects.FIRE_RESISTANCE, 260, 0);
    }

    private void sendAuraParticles(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || player.tickCount % 14 != 0) {
            return;
        }
        ParticleOptions[] particles = {
                ParticleTypes.SCULK_SOUL,
                ParticleTypes.END_ROD,
                ParticleTypes.COMPOSTER
        };
        for (int i = 0; i < particles.length; i++) {
            double angle = (player.tickCount * 0.18D) + i * (Math.PI * 2.0D / particles.length);
            double x = player.getX() + Math.cos(angle) * 0.42D;
            double z = player.getZ() + Math.sin(angle) * 0.42D;
            serverLevel.sendParticles(particles[i], x, player.getY(1.15D), z,
                    2, 0.08D, 0.20D, 0.08D, 0.015D);
        }
        if (player.tickCount % 42 == 0) {
            serverLevel.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY(1.35D), player.getZ(),
                    3, 0.30D, 0.20D, 0.30D, 0.02D);
        }
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return Usless_mobs.MODID + ":textures/models/armor/true_balance_layer_1.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HumanoidModel<?> model;

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot != EquipmentSlot.HEAD) {
                    return original;
                }
                if (model == null) {
                    model = WornTruePathArmorModel.createBalancedCrown();
                }
                ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);
                WornTruePathArmorModel.showCrown(model);
                return model;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(path.pathTooltipKey).withStyle(path.color));
        tooltip.add(Component.translatable(path.guardTooltipKey).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.usless_mobs.true_crown.tooltip.unique").withStyle(ChatFormatting.DARK_RED));
    }
}
