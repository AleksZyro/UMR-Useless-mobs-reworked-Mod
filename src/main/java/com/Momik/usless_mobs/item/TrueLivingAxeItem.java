package com.Momik.usless_mobs.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrueLivingAxeItem extends AxeItem {

    public TrueLivingAxeItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!result) return false;

        boolean hasFullSet = attacker instanceof Player p &&
                TruePathArmorItem.getWornFullSetPath(p) == TruePathArmorItem.Path.LIVING;

        target.addEffect(new MobEffectInstance(MobEffects.POISON, hasFullSet ? 120 : 60, 1, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));

        int regenAmp = hasFullSet ? 1 : 0;
        attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, hasFullSet ? 100 : 40, regenAmp, false, false, true));
        if (hasFullSet) {
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1, false, false, true));
        }

        if (attacker.level() instanceof ServerLevel srv) {
            srv.sendParticles(ParticleTypes.HAPPY_VILLAGER, attacker.getX(), attacker.getY(0.8), attacker.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
            srv.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY(0.7), target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);

            float pitch = 0.9F + srv.getRandom().nextFloat() * 0.2F;
            srv.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.7F, 0.9F * pitch);
            srv.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 0.5F, 0.7F * pitch);
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.true_living_axe.tooltip").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.usless_mobs.path_talisman.requirement").withStyle(ChatFormatting.DARK_GRAY));
    }
}
