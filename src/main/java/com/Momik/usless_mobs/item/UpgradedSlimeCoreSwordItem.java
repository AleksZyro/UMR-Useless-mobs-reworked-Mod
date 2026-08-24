package com.Momik.usless_mobs.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class UpgradedSlimeCoreSwordItem extends SwordItem {
    public enum Path {
        VOID("item.usless_mobs.void_slime_core_sword.tooltip", ChatFormatting.DARK_PURPLE, ParticleTypes.SCULK_SOUL),
        CELESTIAL("item.usless_mobs.celestial_slime_core_sword.tooltip", ChatFormatting.AQUA, ParticleTypes.END_ROD),
        BALANCE("item.usless_mobs.balance_slime_core_sword.tooltip", ChatFormatting.GOLD, ParticleTypes.WAX_ON);

        private final String tooltipKey;
        private final ChatFormatting color;
        private final ParticleOptions particle;

        Path(String tooltipKey, ChatFormatting color, ParticleOptions particle) {
            this.tooltipKey = tooltipKey;
            this.color = color;
            this.particle = particle;
        }
    }

    private final Path path;

    public UpgradedSlimeCoreSwordItem(Tier tier, int attackDamage, float attackSpeed, Path path, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.path = path;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!result) {
            return false;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, path == Path.BALANCE ? 2 : 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, path == Path.VOID ? 1 : 0));
        if (path == Path.VOID) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 0));
        } else if (path == Path.CELESTIAL) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
        }

        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(path.particle, target.getX(), target.getY(0.7D), target.getZ(),
                    16, 0.35D, 0.35D, 0.35D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY(0.55D), target.getZ(),
                    12, 0.3D, 0.25D, 0.3D, 0.03D);
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(path.tooltipKey).withStyle(path.color));
        tooltip.add(Component.translatable("item.usless_mobs.upgraded_slime_core_sword.tooltip.no_knockback").withStyle(ChatFormatting.GRAY));
    }
}
