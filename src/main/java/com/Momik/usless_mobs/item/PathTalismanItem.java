package com.Momik.usless_mobs.item;

import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.client.TalismanRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class PathTalismanItem extends Item implements GeoItem {

    private static final RawAnimation SPIN_ANIM =
            RawAnimation.begin().thenLoop("animation.talisman.spin");

    public enum Path { VOID, CELESTIAL, LIVING }

    private final Path path;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PathTalismanItem(Path path, Properties properties) {
        super(properties);
        this.path = path;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public Path getPath() { return path; }

    /** Called from PlayerTickEvent every 20 ticks when talisman is in inventory. */
    public void applyPassive(Player player) {
        if (!matchesPlayerAllegiance(player)) return;
        switch (path) {
            case VOID ->
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, true, false, true));
            case CELESTIAL ->
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, true, false, true));
            case LIVING ->
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, true, false, true));
        }
        spawnPassiveParticles(player);
    }

    /** Dezente pfadfarbige Funken um die Hueften, signalisiert das aktive Passiv. */
    private void spawnPassiveParticles(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        Vector3f color = switch (path) {
            case VOID      -> new Vector3f(0.72F, 0.30F, 1.00F);
            case CELESTIAL -> new Vector3f(0.35F, 0.85F, 1.00F);
            case LIVING    -> new Vector3f(0.45F, 0.90F, 0.30F);
        };
        DustParticleOptions dust = new DustParticleOptions(color, 0.7F);
        RandomSource rand = player.getRandom();
        for (int i = 0; i < 4; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double radius = 0.5 + rand.nextDouble() * 0.4;
            serverLevel.sendParticles(dust,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.6 + rand.nextDouble() * 0.6,
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.02, 0.0, 0.0);
        }
    }

    private boolean matchesPlayerAllegiance(Player player) {
        AllegiancePath allegiance = AllegianceUtil.getPath(player);
        return switch (path) {
            case VOID      -> allegiance == AllegiancePath.VOID;
            case CELESTIAL -> allegiance == AllegiancePath.CELESTIAL;
            case LIVING    -> allegiance == AllegiancePath.LIVING;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ChatFormatting color = switch (path) {
            case VOID      -> ChatFormatting.DARK_PURPLE;
            case CELESTIAL -> ChatFormatting.AQUA;
            case LIVING    -> ChatFormatting.GREEN;
        };
        tooltip.add(Component.translatable("item.usless_mobs.path_talisman." + path.name().toLowerCase() + ".tooltip")
                .withStyle(color));
        tooltip.add(Component.translatable("item.usless_mobs.path_talisman.requirement")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private TalismanRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) this.renderer = new TalismanRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", 0,
                state -> state.setAndContinue(SPIN_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
