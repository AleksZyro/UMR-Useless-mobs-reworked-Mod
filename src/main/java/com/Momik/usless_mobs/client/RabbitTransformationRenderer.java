package com.Momik.usless_mobs.client;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID, value = Dist.CLIENT)
public final class RabbitTransformationRenderer {
    private RabbitTransformationRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.RABBIT_FORM.get())) {
            return;
        }

        Rabbit rabbit = EntityType.RABBIT.create(player.level());
        if (rabbit == null) {
            return;
        }
        rabbit.tickCount = player.tickCount;
        rabbit.setYRot(player.getYRot());
        rabbit.setXRot(player.getXRot());
        rabbit.yBodyRot = player.yBodyRot;
        rabbit.yBodyRotO = player.yBodyRotO;
        rabbit.yHeadRot = player.yHeadRot;
        rabbit.yHeadRotO = player.yHeadRotO;
        rabbit.setDeltaMovement(player.getDeltaMovement());

        event.getPoseStack().pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher().render(
                rabbit,
                0.0D,
                0.0D,
                0.0D,
                player.getYRot(),
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight());
        event.getPoseStack().popPose();
        event.setCanceled(true);
    }
}
