package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class OceanMobSpawnHandler {
    private static final float CORAL_DROWNED_CHANCE = 0.12F;

    private OceanMobSpawnHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof Drowned drowned)) {
            return;
        }
        if (drowned.getType() != EntityType.DROWNED || drowned.isPersistenceRequired() || drowned.hasCustomName()) {
            return;
        }
        if (!serverLevel.getFluidState(drowned.blockPosition()).is(FluidTags.WATER) || serverLevel.random.nextFloat() >= CORAL_DROWNED_CHANCE) {
            return;
        }

        CoralDrownedEntity coral = com.Momik.usless_mobs.registry.ModEntities.CORAL_DROWNED.get().create(serverLevel);
        if (coral == null) {
            return;
        }

        coral.moveTo(drowned.getX(), drowned.getY(), drowned.getZ(), drowned.getYRot(), drowned.getXRot());
        coral.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(drowned.blockPosition()), MobSpawnType.CONVERSION, null, null);
        event.setCanceled(true);
        serverLevel.addFreshEntity(coral);
    }
}
