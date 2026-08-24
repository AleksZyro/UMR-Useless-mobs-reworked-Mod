package net.mysith.silverfish;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CorruptedCrystalLeggingsItem extends ArmorItem {
    private static final String NEXT_CRYSTAL_GUARD_TICK_KEY = "UslessMobs_CorruptedLeggings_NextGuardTick";
    private static final double CRYSTAL_ARMOR = 10.0D;
    private static final double CRYSTAL_TOUGHNESS = 4.5D;
    private static final double CRYSTAL_KNOCKBACK_RESISTANCE = 0.18D;
    private static final double CRYSTAL_MOVEMENT_SPEED = 0.06D;
    private static final Multimap<Attribute, AttributeModifier> ATTRIBUTES = createAttributes();

    public CorruptedCrystalLeggingsItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, Type.LEGGINGS, properties);
    }

    private static Multimap<Attribute, AttributeModifier> createAttributes() {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        EquipmentSlot slot = EquipmentSlot.LEGS;
        builder.put(Attributes.ARMOR,                com.Momik.usless_mobs.item.UmrAttributes.addition("corrupted_crystal", slot, "armor",     "Corrupted crystal armor",               CRYSTAL_ARMOR));
        builder.put(Attributes.ARMOR_TOUGHNESS,      com.Momik.usless_mobs.item.UmrAttributes.addition("corrupted_crystal", slot, "tough",     "Corrupted crystal toughness",           CRYSTAL_TOUGHNESS));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, com.Momik.usless_mobs.item.UmrAttributes.addition("corrupted_crystal", slot, "knockback", "Corrupted crystal knockback resistance", CRYSTAL_KNOCKBACK_RESISTANCE));
        builder.put(Attributes.MOVEMENT_SPEED,       com.Momik.usless_mobs.item.UmrAttributes.multiplyTotal("corrupted_crystal", slot, "speed", "Corrupted crystal crawl speed",          CRYSTAL_MOVEMENT_SPEED));
        return builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS ? ATTRIBUTES : super.getDefaultAttributeModifiers(slot);
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

        boolean underground = player.blockPosition().getY() <= 48 || !level.canSeeSky(player.blockPosition());
        boolean reactorChestplate = hasReactorChestplate(player);
        if (underground) {
            SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 60, 0);
            SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 60, reactorChestplate ? 1 : 0);
            if (reactorChestplate) {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 60, 0);
            }
            if (level.getMaxLocalRawBrightness(player.blockPosition()) <= 7) {
                SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 240, 0);
            }
            player.fallDistance = Math.min(player.fallDistance, reactorChestplate ? 0.75F : 1.5F);
        }

        if (reactorChestplate) {
            SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 80, 0);
            SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 80, 1);
            if (!level.isClientSide && player.tickCount % 120 == 0 && player.getHealth() < player.getMaxHealth() * 0.75F) {
                player.heal(0.5F);
            }
        }

        tickCrystalGuard(player, reactorChestplate);
        tickBurrowSprint(player, underground, reactorChestplate);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            tickSwarmAura(player, serverLevel, underground, reactorChestplate);
            tickCorruptionPulse(player, serverLevel, reactorChestplate);
            emitCrystalParticles(player, serverLevel, underground, reactorChestplate);
        }
    }

    private static void tickCrystalGuard(Player player, boolean reactorChestplate) {
        if (player.level().isClientSide) {
            return;
        }
        if (player.getHealth() > player.getMaxHealth() * 0.4F) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int now = player.tickCount;
        if (data.getInt(NEXT_CRYSTAL_GUARD_TICK_KEY) > now) {
            return;
        }

        SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, reactorChestplate ? 220 : 180, reactorChestplate ? 1 : 0);
        SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, reactorChestplate ? 100 : 80, 0);
        SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, reactorChestplate ? 120 : 90, 0);
        if (player.hasEffect(MobEffects.DARKNESS)) {
            player.removeEffect(MobEffects.DARKNESS);
        }
        data.putInt(NEXT_CRYSTAL_GUARD_TICK_KEY, now + (reactorChestplate ? 520 : 700));
    }

    private static void tickBurrowSprint(Player player, boolean underground, boolean reactorChestplate) {
        if (!underground || !player.onGround() || !player.isSprinting() || player.tickCount % 8 != 0) {
            return;
        }

        Vec3 lookDirection = player.getLookAngle();
        double strength = reactorChestplate ? 0.12D : 0.08D;
        player.push(lookDirection.x * strength, reactorChestplate ? 0.04D : 0.025D, lookDirection.z * strength);
        player.hasImpulse = true;
    }

    private static void tickSwarmAura(Player player, ServerLevel serverLevel, boolean underground, boolean reactorChestplate) {
        int tick = player.tickCount;
        double radius = reactorChestplate ? 6.0D : underground ? 5.0D : 3.5D;
        AABB aura = player.getBoundingBox().inflate(radius);

        if (tick % 20 == 0) {
            for (Silverfish silverfish : serverLevel.getEntitiesOfClass(Silverfish.class, aura, silverfish -> silverfish.isAlive())) {
                if (silverfish.getTarget() == player) {
                    silverfish.setTarget(null);
                }
                silverfish.addEffect(new MobEffectInstance(MobEffects.GLOWING, 70, 0, true, false, true));
                silverfish.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, reactorChestplate ? 1 : 0));
                if (reactorChestplate) {
                    silverfish.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                }
            }
        }

        if (tick % 60 != 0) {
            return;
        }

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, aura, target -> target.isAlive() && target != player)) {
            if (!(target instanceof Monster monster)) {
                continue;
            }
            if (monster.getTarget() == player && target instanceof CorruptedSilverfishEntity) {
                monster.setTarget(null);
            }
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
            if (underground) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0));
            }
        }

        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY(0.55D), player.getZ(),
                reactorChestplate ? 18 : 10, radius * 0.18D, 0.25D, radius * 0.18D, 0.01D);
    }

    private static void tickCorruptionPulse(Player player, ServerLevel serverLevel, boolean reactorChestplate) {
        int interval = reactorChestplate ? 80 : 100;
        if (player.tickCount % interval != 0) {
            return;
        }

        int radius = reactorChestplate ? 24 : 18;
        CorruptedSilverfishTracker.SearchResult result = CorruptedSilverfishTracker.findNearest(serverLevel, player, radius);
        if (result == null || result.distance() > radius) {
            return;
        }

        BlockPos pos = result.pos();
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                reactorChestplate ? 16 : 10, 0.25D, 0.20D, 0.25D, 0.0D);
        if (reactorChestplate && result.hidden()) {
            CorruptedSilverfishTracker.reveal(serverLevel, pos, false);
        }
    }

    private static void emitCrystalParticles(Player player, ServerLevel serverLevel, boolean underground, boolean reactorChestplate) {
        if (player.tickCount % (reactorChestplate ? 4 : 8) != 0) {
            return;
        }

        double spread = underground ? 0.45D : 0.25D;
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY(0.7D), player.getZ(),
                reactorChestplate ? 3 : 1, spread, 0.15D, spread, 0.015D);
    }

    private static boolean hasReactorChestplate(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(com.Momik.usless_mobs.registry.ModItems.SCHLEIMREAKTOR_BRUSTPANZER.get());
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "usless_mobs:textures/models/armor/corrupted_crystal_layer_2.png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.corrupted_crystal_leggings.tooltip.stats").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.usless_mobs.corrupted_crystal_leggings.tooltip.cave").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.usless_mobs.corrupted_crystal_leggings.tooltip.guard").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.usless_mobs.corrupted_crystal_leggings.tooltip.swarm").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.usless_mobs.corrupted_crystal_leggings.tooltip.sense").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.usless_mobs.corrupted_crystal_leggings.tooltip.set_bonus").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
