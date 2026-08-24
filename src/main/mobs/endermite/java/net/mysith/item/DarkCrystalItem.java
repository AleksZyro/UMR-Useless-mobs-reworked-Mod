package net.mysith.item;

import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DarkCrystalItem extends Item {
    public DarkCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof EnderSlimeEntity enderSlime && !(target instanceof CelestialSlimeEntity)) {
            return awakenCelestialSlime(stack, player, hand, enderSlime);
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult awakenCelestialSlime(ItemStack stack, Player player, InteractionHand hand, EnderSlimeEntity enderSlime) {
        Level level = enderSlime.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        CelestialSlimeEntity celestial = com.Momik.usless_mobs.registry.ModEntities.CELESTIAL_SLIME.get().create(serverLevel);
        if (celestial == null) {
            return InteractionResult.PASS;
        }

        int size = Math.max(3, enderSlime.getSize());
        celestial.moveTo(enderSlime.getX(), enderSlime.getY(), enderSlime.getZ(), enderSlime.getYRot(), enderSlime.getXRot());
        celestial.setSize(size, true);
        celestial.setYHeadRot(enderSlime.getYHeadRot());
        celestial.setPersistenceRequired();
        if (enderSlime.getTarget() != null) {
            celestial.setTarget(enderSlime.getTarget());
        }

        if (!serverLevel.addFreshEntity(celestial)) {
            return InteractionResult.FAIL;
        }
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                enderSlime.getX(), enderSlime.getY(0.65D), enderSlime.getZ(),
                52, 0.70D, 0.45D, 0.70D, 0.06D);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                enderSlime.getX(), enderSlime.getY(0.55D), enderSlime.getZ(),
                46, 0.55D, 0.65D, 0.55D, 0.30D);
        serverLevel.playSound(null, enderSlime.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.25F, 0.65F);
        serverLevel.playSound(null, enderSlime.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0F, 0.55F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.swing(hand, true);
        player.displayClientMessage(Component.translatable("item.usless_mobs.dark_crystal.used"), true);
        enderSlime.discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.dark_crystal.tooltip1").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.usless_mobs.dark_crystal.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
