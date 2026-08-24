package net.mysith.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mysith.effect.DeathMarkEffect;
import net.mysith.effect.ReaperMarkEffect;
import net.mysith.MySithMod;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MySithMod.MODID);

    public static final RegistryObject<MobEffect> REAPERS_MARK =
            EFFECTS.register("reapers_mark", ReaperMarkEffect::new);

    public static final RegistryObject<MobEffect> DEATH_MARK =
            EFFECTS.register("death_mark", DeathMarkEffect::new);
}
