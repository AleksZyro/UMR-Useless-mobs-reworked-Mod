package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class IceArrowItem extends ArrowItem {

    public IceArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        Arrow arrow = new Arrow(level, shooter) {
            @Override
            protected void doPostHurtEffects(LivingEntity target) {
                super.doPostHurtEffects(target);
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 240));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 3));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.8F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                            target.getX(), target.getY(0.6D), target.getZ(),
                            28, 0.35D, 0.45D, 0.35D, 0.04D);
                }
            }

            @Override
            protected ItemStack getPickupItem() {
                return new ItemStack(com.Momik.usless_mobs.registry.ModItems.ICE_ARROW.get());
            }
        };
        arrow.setBaseDamage(arrow.getBaseDamage() + 0.5D);
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.ice_arrow.tooltip"));
    }
}
