package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.registry.ModEntities;
import com.Momik.usless_mobs.registry.ModItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mysith.client.CorruptedSilverfishRenderer;

@Mod.EventBusSubscriber(
        modid = Usless_mobs.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLAUER_SCHLEIM.get(), BlueSlimeRenderer::createRenderer);
        event.registerEntityRenderer(ModEntities.KING_SCHLEIM.get(), KingSlimeRenderer::createRenderer);
        event.registerEntityRenderer(ModEntities.ENDER_SCHLEIM.get(), EnderSlimeRenderer::createRenderer);
        event.registerEntityRenderer(ModEntities.CELESTIAL_SLIME.get(), CelestialSlimeRenderer::createRenderer);
        event.registerEntityRenderer(ModEntities.CORRUPTED_SILVERFISH.get(), CorruptedSilverfishRenderer::createRenderer);
        event.registerEntityRenderer(ModEntities.LIVING_BOSS.get(), LivingBossRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_STRAY.get(), FrostStrayRenderer::new);
        event.registerEntityRenderer(ModEntities.WEB_CAVE_SPIDER.get(), WebCaveSpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.CORAL_DROWNED.get(), CoralDrownedRenderer::new);
        event.registerEntityRenderer(ModEntities.OCTOPUS.get(), OctopusRenderer::new);
        event.registerEntityRenderer(ModEntities.WITCH_BOSS.get(), WitchBossRenderer::new);
        event.registerEntityRenderer(ModEntities.HELPING_ALLAY.get(), HelpingAllayRenderer::new);
        event.registerEntityRenderer(ModEntities.LIVING_SQUID.get(), LivingSquidRenderer::new);
        event.registerEntityRenderer(ModEntities.GIANT_SQUID.get(), GiantSquidRenderer::new);
        event.registerEntityRenderer(ModEntities.LIVING_GLOW_SQUID.get(), LivingGlowSquidRenderer::new);
        event.registerEntityRenderer(ModEntities.LIVING_POLAR_BEAR.get(), LivingPolarBearRenderer::new);
        event.registerEntityRenderer(ModEntities.LIVING_AXOLOTL.get(), LivingAxolotlRenderer::new);
        event.registerEntityRenderer(ModEntities.LIVING_OCELOT.get(), LivingOcelotRenderer::new);
        event.registerEntityRenderer(ModEntities.LIVING_BAT.get(), LivingBatRenderer::new);
        event.registerEntityRenderer(ModEntities.ROOTED_HUSK.get(), RootedHuskRenderer::new);
        event.registerEntityRenderer(ModEntities.SLIME_SPIKE.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CustomMobModelLayers.LIVING_BOSS,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.LIVING_BOSS));
        event.registerLayerDefinition(CustomMobModelLayers.FROST_STRAY,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.FROST_STRAY));
        event.registerLayerDefinition(CustomMobModelLayers.WEB_CAVE_SPIDER,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.WEB_CAVE_SPIDER));
        event.registerLayerDefinition(CustomMobModelLayers.CORAL_DROWNED,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.CORAL_DROWNED));
        event.registerLayerDefinition(CustomMobModelLayers.OCTOPUS,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.OCTOPUS));
        event.registerLayerDefinition(CustomMobModelLayers.WITCH_BOSS,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.WITCH_BOSS));
        event.registerLayerDefinition(CustomMobModelLayers.LIVING_BAT,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.LIVING_BAT));
        event.registerLayerDefinition(CustomMobModelLayers.ROOTED_HUSK,
                () -> CustomMob3DModel.createLayer(CustomMob3DModel.Variant.ROOTED_HUSK));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_SLIME_EFFECTS);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.SLIME_KOMPASS.get(),
                ResourceLocation.tryBuild(Usless_mobs.MODID, "slime_kompass_angle"),
                (stack, level, livingEntity, seed) -> {
                    if (livingEntity == null || stack.getTag() == null
                            || !stack.getTag().contains("TargetX") || !stack.getTag().contains("TargetZ")) {
                        return 0.0F;
                    }

                    double dx = stack.getTag().getInt("TargetX") + 0.5D - livingEntity.getX();
                    double dz = stack.getTag().getInt("TargetZ") + 0.5D - livingEntity.getZ();
                    if (dx * dx + dz * dz < 0.0001D) {
                        return 0.0F;
                    }

                    double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
                    double relativeAngle = Mth.wrapDegrees(targetYaw - livingEntity.getYRot()) / 360.0D;
                    return (float) (relativeAngle < 0.0D ? relativeAngle + 1.0D : relativeAngle);
                }));
    }
}
