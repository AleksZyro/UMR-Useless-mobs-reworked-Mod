package net.mysith.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModEffects;

/**
 * Renders the original floating Minecraft skeleton skull above Death-Marked mobs.
 * Client-only — registered on the Forge bus via @EventBusSubscriber(value = Dist.CLIENT).
 */
@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID, value = Dist.CLIENT)
public class DeathMarkRenderHandler {

    private static final Map<Integer, Long> syncedMarkedEntities = new HashMap<>();
    private static ItemStack skullStack;

    private static ItemStack getSkullStack() {
        if (skullStack == null) skullStack = new ItemStack(Items.SKELETON_SKULL);
        return skullStack;
    }

    public static void markEntity(int entityId, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        syncedMarkedEntities.put(entityId, mc.level.getGameTime() + durationTicks);
    }

    private static boolean shouldRenderDeathMark(LivingEntity entity) {
        if (entity.hasEffect(ModEffects.DEATH_MARK.get())) return true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Long expiryTick = syncedMarkedEntities.get(entity.getId());
        if (expiryTick == null) return false;

        if (entity.isRemoved() || mc.level.getGameTime() > expiryTick) {
            syncedMarkedEntities.remove(entity.getId());
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!shouldRenderDeathMark(entity)) return;

        Minecraft mc = Minecraft.getInstance();
        // Hide only the local player's own marker; other marked players still render normally.
        if (entity == mc.player) return;

        PoseStack pose = event.getPoseStack();
        float ageTick = entity.tickCount + event.getPartialTick();

        pose.pushPose();

        float bob = (float) Math.sin(ageTick * 0.12) * 0.08f;
        double yOffset = entity.getBbHeight() + 0.7 + bob;
        pose.translate(0.0, yOffset, 0.0);

        pose.mulPose(Axis.YP.rotationDegrees((ageTick * 3.0F) % 360.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-15.0F));

        float entityScale = Math.max(0.78F, Math.min(1.08F, entity.getBbHeight() / 3.2F));
        float pulse = entityScale * (0.82F + (float) Math.sin(ageTick * 0.16F) * 0.035F);
        pose.scale(pulse, pulse, pulse);

        mc.getItemRenderer().renderStatic(
                getSkullStack(),
                ItemDisplayContext.GROUND,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                pose,
                event.getMultiBufferSource(),
                entity.level(),
                entity.getId()
        );

        pose.popPose();
    }
}
