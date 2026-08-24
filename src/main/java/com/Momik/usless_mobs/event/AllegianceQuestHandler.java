package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceStage;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.entity.CelestialSlimeEntity;
import com.Momik.usless_mobs.entity.EnderSlimeEntity;
import com.Momik.usless_mobs.entity.KingSlimeEntity;
import com.Momik.usless_mobs.entity.LivingBossEntity;
import com.Momik.usless_mobs.item.PathTalismanItem;
import com.Momik.usless_mobs.item.TruePathArmorItem;
import com.Momik.usless_mobs.registry.ModEntities;
import com.Momik.usless_mobs.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class AllegianceQuestHandler {

    private static final int VOID_TRIAL_KILLS      = 5;
    private static final int CELESTIAL_TRIAL_KILLS = 5;
    private static final int LIVING_TRIAL_KILLS    = 3;
    private static final int MASTERY_KILLS         = 30;

    private AllegianceQuestHandler() {}

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        AllegianceStage stage = AllegianceUtil.getStage(player);

        // Stage NONE → INITIATED: Kill King Slime for the first time
        if (stage == AllegianceStage.NONE && event.getEntity() instanceof KingSlimeEntity) {
            AllegianceUtil.setStage(player, AllegianceStage.INITIATED);
            KingSlimeAdvancements.grant(player, "allegiance/initiated");
            player.displayClientMessage(Component.translatable("quest.usless_mobs.initiated")
                    .withStyle(ChatFormatting.GOLD), false);
        }

        // Stage PATH_CHOSEN → PATH_TRIAL: Kill path-specific boss N times
        if (stage == AllegianceStage.PATH_CHOSEN) {
            AllegiancePath path = AllegianceUtil.getPath(player);
            int required = switch (path) {
                case VOID      -> VOID_TRIAL_KILLS;
                case CELESTIAL -> CELESTIAL_TRIAL_KILLS;
                case LIVING    -> LIVING_TRIAL_KILLS;
                default -> 0;
            };
            boolean isTarget = switch (path) {
                case VOID      -> event.getEntity() instanceof EnderSlimeEntity;
                case CELESTIAL -> event.getEntity() instanceof CelestialSlimeEntity;
                case LIVING    -> event.getEntity() instanceof LivingBossEntity;
                default -> false;
            };
            if (isTarget && required > 0) {
                int count = AllegianceUtil.incrementPathKills(player);
                player.displayClientMessage(
                        Component.translatable("quest.usless_mobs.trial_progress", count, required)
                                .withStyle(ChatFormatting.DARK_AQUA), true);
                if (count >= required) {
                    AllegianceUtil.setStage(player, AllegianceStage.PATH_TRIAL);
                    AllegianceUtil.resetPathKills(player);
                    KingSlimeAdvancements.grant(player, "allegiance/" + path.name().toLowerCase() + "_trial");
                    player.displayClientMessage(Component.translatable("quest.usless_mobs.trial_complete")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), false);
                }
            }
        }

        // Stage ASCENDED → MASTERED: Kill 30 enemies while wearing full True Path set
        if (stage == AllegianceStage.ASCENDED) {
            TruePathArmorItem.Path armorPath = TruePathArmorItem.getWornFullSetPath(player);
            if (armorPath != null) {
                int count = AllegianceUtil.incrementSetKills(player);
                if (count >= MASTERY_KILLS) {
                    AllegianceUtil.setStage(player, AllegianceStage.MASTERED);
                    KingSlimeAdvancements.grant(player, "allegiance/mastered");
                    if (player.getServer() != null) {
                        player.getServer().getPlayerList().broadcastSystemMessage(
                                Component.translatable("quest.usless_mobs.mastered_broadcast",
                                        player.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
                    }
                    spawnRitualGuardians(player);
                }
            }
        }
    }

    private static void spawnRitualGuardians(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        AllegiancePath path = AllegianceUtil.getPath(player);
        Component guardianName = Component.literal("Ascendant Guardian").withStyle(ChatFormatting.GOLD);

        for (int i = 0; i < 2; i++) {
            double angle = i * Math.PI;
            double x = player.getX() + Math.cos(angle) * 5;
            double z = player.getZ() + Math.sin(angle) * 5;

            Mob guardian = switch (path) {
                case VOID      -> new EnderSlimeEntity(ModEntities.ENDER_SCHLEIM.get(), serverLevel);
                case CELESTIAL -> new CelestialSlimeEntity(ModEntities.CELESTIAL_SLIME.get(), serverLevel);
                case LIVING    -> new LivingBossEntity(ModEntities.LIVING_BOSS.get(), serverLevel);
                default        -> new KingSlimeEntity(ModEntities.KING_SCHLEIM.get(), serverLevel);
            };
            guardian.setCustomName(guardianName);
            guardian.setCustomNameVisible(true);
            guardian.moveTo(x, player.getY(), z, 0, 0);
            serverLevel.addFreshEntity(guardian);
        }

        player.displayClientMessage(
                Component.literal("The path stirs... Ascendant Guardians have awakened!")
                        .withStyle(ChatFormatting.GOLD), false);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (AllegianceUtil.getStage(player) != AllegianceStage.PATH_TRIAL) return;

        TruePathArmorItem.Path armorPath = TruePathArmorItem.getWornFullSetPath(player);
        if (armorPath == null) return;

        AllegiancePath allegiance = AllegianceUtil.getPath(player);
        boolean matches = switch (allegiance) {
            case VOID      -> armorPath == TruePathArmorItem.Path.VOID;
            case CELESTIAL -> armorPath == TruePathArmorItem.Path.CELESTIAL;
            case LIVING    -> armorPath == TruePathArmorItem.Path.LIVING;
            default -> false;
        };
        if (!matches) return;

        AllegianceUtil.setStage(player, AllegianceStage.ASCENDED);
        KingSlimeAdvancements.grant(player, "allegiance/ascended");
        if (player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("quest.usless_mobs.ascended_broadcast",
                            player.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 100 != 0) return;

        AllegianceStage stage = AllegianceUtil.getStage(player);

        // Apply talisman passive effect if in inventory
        for (var talisman : new net.minecraftforge.registries.RegistryObject[]{
                ModItems.VOID_TALISMAN, ModItems.CELESTIAL_TALISMAN, ModItems.LIVING_TALISMAN}) {
            if (player.getInventory().contains(new net.minecraft.world.item.ItemStack(
                    (net.minecraft.world.item.Item) talisman.get()))) {
                ((PathTalismanItem) talisman.get()).applyPassive(player);
                break;
            }
        }

        if (stage == AllegianceStage.NONE || stage == AllegianceStage.MASTERED) return;

        // Show current quest hint in action bar once per 5 seconds
        AllegiancePath path = AllegianceUtil.getPath(player);
        String hintKey = switch (stage) {
            case NONE      -> "quest.usless_mobs.hint.none";
            case INITIATED -> "quest.usless_mobs.hint.initiated";
            case PATH_CHOSEN -> "quest.usless_mobs.hint.path_chosen." + path.name().toLowerCase();
            case PATH_TRIAL  -> "quest.usless_mobs.hint.path_trial";
            case ASCENDED    -> "quest.usless_mobs.hint.ascended";
            default          -> null;
        };
        if (hintKey != null) {
            player.displayClientMessage(
                    Component.translatable(hintKey).withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }
}
