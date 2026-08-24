package net.mysith.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Reaper's Mark: thematic replacement for Wither when inflicted by Reaper-themed sources.
 * Deals magic damage on a ticking interval, scaled by amplifier (same cadence as vanilla Wither).
 * The visual layer (red sparks around the marked entity) is handled by ReaperMarkTickHandler.
 */
public class ReaperMarkEffect extends MobEffect {

    public ReaperMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0010);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 40 >> amplifier;
        return interval == 0 || duration % interval == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 1 dmg per interval (matches Wither cadence). Source: magic, so kill messages stay thematic.
        entity.hurt(entity.damageSources().magic(), 1.0F);
    }
}
