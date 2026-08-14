package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TruePathSwordItem extends SwordItem {

    public enum Path {
        VOID(ChatFormatting.DARK_PURPLE, AllegiancePath.VOID, "item.usless_mobs.true_void_sword.tooltip"),
        CELESTIAL(ChatFormatting.AQUA, AllegiancePath.CELESTIAL, "item.usless_mobs.true_celestial_sword.tooltip");

        final ChatFormatting color;
        final AllegiancePath allegiancePath;
        final String tooltipKey;

        Path(ChatFormatting color, AllegiancePath allegiancePath, String tooltipKey) {
            this.color = color;
            this.allegiancePath = allegiancePath;
            this.tooltipKey = tooltipKey;
        }
    }

    private final Path path;

    public TruePathSwordItem(Tier tier, int baseDamage, float attackSpeed, Path path, Properties properties) {
        super(tier, baseDamage, attackSpeed, properties);
        this.path = path;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!result) return false;

        boolean hasFullSet = attacker instanceof Player p && TruePathArmorItem.getWornFullSetPath(p) != null &&
                TruePathArmorItem.getWornFullSetPath(p).name().equals(path.name());

        if (path == Path.VOID) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
            float healAmount = hasFullSet ? 5.0f : 2.0f;
            if (attacker instanceof Player p) p.heal(healAmount);
        } else {
            // CELESTIAL
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true, true));
            int speedAmp = hasFullSet ? 1 : 0;
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, speedAmp, false, false, true));
            if (hasFullSet) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true));
            }
        }

        if (attacker.level() instanceof ServerLevel srv) {
            var particle = path == Path.VOID ?
                    net.minecraft.core.particles.ParticleTypes.SCULK_SOUL :
                    net.minecraft.core.particles.ParticleTypes.END_ROD;
            srv.sendParticles(particle, target.getX(), target.getY(0.7), target.getZ(), 12, 0.3, 0.3, 0.3, 0.04);

            float pitch = 0.9F + srv.getRandom().nextFloat() * 0.2F;
            if (path == Path.VOID) {
                srv.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.35F, 1.6F * pitch);
                srv.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6F, 0.7F * pitch);
            } else {
                srv.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.1F * pitch);
                srv.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.5F * pitch);
            }
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(path.tooltipKey).withStyle(path.color));
        tooltip.add(Component.translatable("item.usless_mobs.path_talisman.requirement")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
