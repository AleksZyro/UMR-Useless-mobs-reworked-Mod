package net.mysith.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.mysith.item.SoulCompassItem;
import net.mysith.MySithMod;
import net.mysith.registry.ModItems;

public class SoulCompassPropertyRegistry {

    private static double rotation = 0;
    private static double rotationSpeed = 0;
    private static long lastTick = 0;

    public static void register() {
        ItemProperties.register(ModItems.SOUL_COMPASS.get(),
                ResourceLocation.tryBuild(MySithMod.MODID, "angle"),
                (stack, level, entity, seed) -> compute(stack, entity));
    }

    private static float compute(ItemStack stack, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return random();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(SoulCompassItem.HAS_TARGET)) return random();

        // Dimension-Check über Player (nur Player haben level())
        String targetDim = tag.getString(SoulCompassItem.TARGET_DIM);
        if (living instanceof Player p) {
            String playerDim = p.level().dimension().location().toString();
            if (!playerDim.equals(targetDim)) return random();
        }

        double tx = tag.getDouble(SoulCompassItem.TARGET_X);
        double tz = tag.getDouble(SoulCompassItem.TARGET_Z);

        Vec3 pos = living.position();
        double dx = tx - pos.x;
        double dz = tz - pos.z;

        double angleToTarget = Math.atan2(dz, dx);
        double yaw = Math.toRadians(living.getYHeadRot() + 90.0); // +90 because vanilla compass uses this offset

        double relative = wrap(angleToTarget - yaw) / (Math.PI * 2);
        return (float) relative;
    }

    private static float random() {
        long now = System.currentTimeMillis();
        if (now - lastTick > 50) {
            rotationSpeed = rotationSpeed * 0.85 + (Math.random() - 0.5) * 0.1;
            rotation = wrap(rotation + rotationSpeed);
            lastTick = now;
        }
        return (float) (rotation / (Math.PI * 2));
    }

    private static double wrap(double a) {
        while (a < -Math.PI) a += Math.PI * 2;
        while (a >= Math.PI) a -= Math.PI * 2;
        return a;
    }
}
