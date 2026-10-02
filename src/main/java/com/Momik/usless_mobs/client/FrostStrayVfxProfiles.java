package com.Momik.usless_mobs.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;

/** Small declarative profile loader. It intentionally limits the PoC to approved vanilla particles. */
final class FrostStrayVfxProfiles {
    private static final ResourceLocation PROFILE_FILE = ResourceLocation.tryBuild(
            com.Momik.usless_mobs.Usless_mobs.MODID, "vfx_profiles/frost_stray_volley.json");
    private static Profile charge;
    private static Profile release;
    private static Profile impact;

    private FrostStrayVfxProfiles() {
    }

    static Profile charge() {
        load();
        return charge;
    }

    static Profile release() {
        load();
        return release;
    }

    static Profile impact() {
        load();
        return impact;
    }

    private static void load() {
        if (charge != null) {
            return;
        }
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(PROFILE_FILE);
            if (resource.isPresent()) {
                try (Reader reader = resource.get().openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    charge = parse(root.getAsJsonObject("charge"));
                    release = parse(root.getAsJsonObject("release"));
                    impact = parse(root.getAsJsonObject("impact"));
                    return;
                }
            }
        } catch (IOException | IllegalStateException ignored) {
            // Fallback keeps a missing cosmetic resource from breaking gameplay.
        }
        charge = new Profile(ParticleTypes.SNOWFLAKE, 4, 1, 2, 0.18D, 0.025D, true);
        release = new Profile(ParticleTypes.ITEM_SNOWBALL, 12, 4, 1, 0.35D, 0.04D, true);
        impact = new Profile(ParticleTypes.SNOWFLAKE, 18, 6, 1, 0.45D, 0.055D, true);
    }

    private static Profile parse(JsonObject object) {
        String particleName = object.get("particle").getAsString();
        ParticleOptions particle = switch (particleName) {
            case "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "item_snowball" -> ParticleTypes.ITEM_SNOWBALL;
            case "cloud" -> ParticleTypes.CLOUD;
            default -> throw new IllegalStateException("unapproved Frost Stray VFX particle: " + particleName);
        };
        return new Profile(particle, object.get("near_count").getAsInt(), object.get("mid_count").getAsInt(),
                object.get("emit_every_ticks").getAsInt(), object.get("spread").getAsDouble(),
                object.get("vertical_speed").getAsDouble(), object.get("important").getAsBoolean());
    }

    record Profile(ParticleOptions particle, int nearCount, int midCount, int emitEveryTicks,
                   double spread, double verticalSpeed, boolean important) {
        Profile {
            if (nearCount < 0 || midCount < 0 || emitEveryTicks <= 0 || spread < 0.0D || verticalSpeed < 0.0D) {
                throw new IllegalArgumentException("invalid Frost Stray VFX profile");
            }
        }

        int countAt(double distanceSquared, boolean oneShot) {
            if (distanceSquared <= 12.0D * 12.0D) {
                return nearCount;
            }
            if (distanceSquared <= 24.0D * 24.0D) {
                return important ? Math.max(1, midCount) : midCount;
            }
            return oneShot && important && distanceSquared <= 32.0D * 32.0D ? 1 : 0;
        }
    }
}
