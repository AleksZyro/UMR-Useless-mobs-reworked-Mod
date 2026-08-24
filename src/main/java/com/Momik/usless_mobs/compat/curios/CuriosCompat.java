package com.Momik.usless_mobs.compat.curios;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

// Curios integration. NEVER reference this class without first checking
// ModList.get().isLoaded("curios") — otherwise classloading will fail when
// Curios is absent because of the top.theillusivec4.* imports.
public final class CuriosCompat {

    private static final ResourceLocation CAP_ID =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "king_slime_crown_curio");
    private static final ResourceLocation BEARCLAW_CAP_ID =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "bearclaw_necklace_curio");
    private static final ResourceLocation PATH_CROWN_CAP_ID =
            ResourceLocation.tryBuild(Usless_mobs.MODID, "path_crown_curio");

    private CuriosCompat() {}

    public static void init(IEventBus modEventBus) {
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, CuriosCompat::onAttachCapabilities);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(CuriosCompat::onClientSetup);
        }
    }

    private static void onAttachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.getItem() == com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get()
                || stack.getItem() == com.Momik.usless_mobs.registry.ModItems.NETHERITE_KINGS_KRONE.get()) {
            ICurio curio = new KingSlimeCrownCurio(stack);
            event.addCapability(CAP_ID, new SimpleCurioProvider(curio));
        }
        if (stack.getItem() == com.Momik.usless_mobs.registry.ModItems.BEARCLAW_NECKLACE.get()
                || stack.getItem() == com.Momik.usless_mobs.registry.ModItems.AWAKENED_BEARCLAW_NECKLACE.get()) {
            ICurio curio = new BearclawNecklaceCurio(stack);
            event.addCapability(BEARCLAW_CAP_ID, new SimpleCurioProvider(curio));
        }
        if (stack.getItem() instanceof com.Momik.usless_mobs.item.PathCrownItem crown) {
            ICurio curio = new PathCrownCurio(stack, crown.getPath());
            event.addCapability(PATH_CROWN_CAP_ID, new SimpleCurioProvider(curio));
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CuriosRendererRegistry.register(com.Momik.usless_mobs.registry.ModItems.KING_SLIME_KRONE.get(), KingSlimeCrownCurioRenderer::new);
            CuriosRendererRegistry.register(com.Momik.usless_mobs.registry.ModItems.NETHERITE_KINGS_KRONE.get(), KingSlimeCrownCurioRenderer::new);
            CuriosRendererRegistry.register(com.Momik.usless_mobs.registry.ModItems.VOID_REAPER_KING.get(), KingSlimeCrownCurioRenderer::new);
            CuriosRendererRegistry.register(com.Momik.usless_mobs.registry.ModItems.GOD_KING.get(), KingSlimeCrownCurioRenderer::new);
            CuriosRendererRegistry.register(com.Momik.usless_mobs.registry.ModItems.LIVING_KING.get(), KingSlimeCrownCurioRenderer::new);
        });
    }

    // Minimal ICapabilityProvider wrapping a single ICurio instance.
    private static final class SimpleCurioProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider {
        private final net.minecraftforge.common.util.LazyOptional<ICurio> opt;

        SimpleCurioProvider(ICurio curio) {
            this.opt = net.minecraftforge.common.util.LazyOptional.of(() -> curio);
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap,
                net.minecraft.core.Direction side) {
            return CuriosCapability.ITEM.orEmpty(cap, opt);
        }
    }
}
