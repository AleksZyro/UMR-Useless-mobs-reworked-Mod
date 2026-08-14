package net.mysith.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mysith.client.particle.SoulSlashParticle;
import net.mysith.registry.ModEntities;
import net.mysith.registry.ModParticles;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.SOUL_ENDERMITE.get(), SoulEndermiteRenderer::new);
        EntityRenderers.register(ModEntities.VOID_REAPER.get(), VoidReaperRenderer::createRenderer);
        // Custom ScytheItemEntity nutzt den Vanilla-ItemEntityRenderer.
        EntityRenderers.register(ModEntities.SCYTHE_ITEM.get(),
                net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.enqueueWork(SoulCompassPropertyRegistry::register);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SOUL_SLASH.get(), SoulSlashParticle.Provider::new);
    }
}
