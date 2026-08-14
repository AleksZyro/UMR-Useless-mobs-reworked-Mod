package net.mysith.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Death Mark: pure marker effect — no damage tick. The visual (a skull billboard
 * above the entity) lives in {@link net.mysith.event.DeathMarkRenderHandler}.
 * The damage bonus is applied in {@link net.mysith.event.EnchantmentHandlers}.
 */
public class DeathMarkEffect extends MobEffect {
    public DeathMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x2D0006);
    }
}
