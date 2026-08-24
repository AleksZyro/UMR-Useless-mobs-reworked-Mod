package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class KingSlimeAdvancements {
    private static final ResourceLocation ENTER_NETHER = ResourceLocation.tryBuild("minecraft", "story/enter_the_nether");
    public static final String SLIME_HUNTER = "slime_hunter";
    public static final String SUMMON_KING_SLIME = "summon_king_slime";
    public static final String HARD_KING_SLIME = "hard_king_slime";

    private KingSlimeAdvancements() {}

    public static void grant(ServerPlayer player, String id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.tryBuild(Usless_mobs.MODID, id));
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }

    public static boolean hasEnteredNether(ServerPlayer player) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(ENTER_NETHER);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
