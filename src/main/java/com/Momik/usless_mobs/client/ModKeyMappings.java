package com.Momik.usless_mobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + Usless_mobs.MODID;

    public static final KeyMapping TOGGLE_SLIME_EFFECTS = new KeyMapping(
            "key.usless_mobs.toggle_slime_effects",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    private ModKeyMappings() {}
}
