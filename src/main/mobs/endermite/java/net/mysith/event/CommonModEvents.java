package net.mysith.event;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.entity.SoulEndermite;
import net.mysith.entity.VoidReaperEntity;
import net.mysith.registry.ModEntities;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {
    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SOUL_ENDERMITE.get(), SoulEndermite.createAttributes().build());
        event.put(ModEntities.VOID_REAPER.get(), VoidReaperEntity.createAttributes().build());
    }
}
