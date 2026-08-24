package net.mysith.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.event.ScytheCraftHandler;
import net.mysith.world.ScytheTracker;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class MysithCommand {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("mysith")
                .requires(s -> s.hasPermission(2))
                // counter <set|get|reset> [value]
                .then(Commands.literal("counter")
                        .then(Commands.literal("get").executes(MysithCommand::getCounter))
                        .then(Commands.literal("reset").executes(c -> setCounter(c, 0)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(c -> setCounter(c, IntegerArgumentType.getInteger(c, "value"))))))
                // kills <set|get>
                .then(Commands.literal("kills")
                        .then(Commands.literal("get").executes(MysithCommand::getKills))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(c -> setKills(c, IntegerArgumentType.getInteger(c, "value"))))))
                // stacks <set|get>
                .then(Commands.literal("stacks")
                        .then(Commands.literal("get").executes(MysithCommand::getStacks))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 10))
                                        .executes(c -> setStacks(c, IntegerArgumentType.getInteger(c, "value"))))))
                // tracker <show|clear>
                .then(Commands.literal("tracker")
                        .then(Commands.literal("show").executes(MysithCommand::showTracker))
                        .then(Commands.literal("clear").executes(MysithCommand::clearTracker)))
                // stats - personal stats
                .then(Commands.literal("stats").executes(MysithCommand::showStats))
                // leaderboard / top
                .then(Commands.literal("top").executes(MysithCommand::showLeaderboard))
                .then(Commands.literal("leaderboard").executes(MysithCommand::showLeaderboard))
        );
    }

    private static int showStats(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        net.mysith.world.ReaperStats stats = net.mysith.world.ReaperStats.get(p.getServer().overworld());
        net.mysith.world.ReaperStats.Entry e = stats.getOrCreate(p.getUUID(), p.getName().getString());
        int curStreak = p.getPersistentData().getInt("MysithCurrentStreak");

        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stats.title", p.getName().getString()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stats.souls_harvested", e.kills).withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stats.current_streak", curStreak).withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stats.best_streak", e.bestStreak).withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stats.deaths_to_scythe", e.deathsToScythe).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int showLeaderboard(CommandContext<CommandSourceStack> ctx) {
        net.mysith.world.ReaperStats stats = net.mysith.world.ReaperStats.get(ctx.getSource().getServer().overworld());
        java.util.List<net.mysith.world.ReaperStats.Entry> top = stats.getTopByKills(10);
        if (top.isEmpty() || top.get(0).kills <= 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.leaderboard.empty").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.leaderboard.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        for (int i = 0; i < top.size(); i++) {
            int rank = i + 1;
            net.mysith.world.ReaperStats.Entry e = top.get(i);
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.leaderboard.entry", rank, e.name, e.kills, e.bestStreak)
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return top.size();
    }

    private static int setCounter(CommandContext<CommandSourceStack> ctx, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        p.getPersistentData().putInt(ScytheCraftHandler.REJECTION_COUNT_KEY, value);
        p.getPersistentData().putLong(ScytheCraftHandler.LAST_REJECTION_TICK_KEY, 0L);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.counter.set", value).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int getCounter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int v = p.getPersistentData().getInt(ScytheCraftHandler.REJECTION_COUNT_KEY);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.counter.get", v).withStyle(ChatFormatting.GRAY), false);
        return v;
    }

    private static int setKills(CommandContext<CommandSourceStack> ctx, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        p.getPersistentData().putInt("MysithReaperKills", value);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.kills.set", value).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int getKills(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int v = p.getPersistentData().getInt("MysithReaperKills");
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.kills.get", v).withStyle(ChatFormatting.GRAY), false);
        return v;
    }

    private static int setStacks(CommandContext<CommandSourceStack> ctx, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        p.getPersistentData().putInt("MysithSoulStacks", value);
        p.getPersistentData().putLong("MysithLastKill", p.level().getGameTime());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stacks.set", value).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int getStacks(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int v = p.getPersistentData().getInt("MysithSoulStacks");
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.stacks.get", v).withStyle(ChatFormatting.GRAY), false);
        return v;
    }

    private static int showTracker(CommandContext<CommandSourceStack> ctx) {
        ScytheTracker t = ScytheTracker.get(ctx.getSource().getServer().overworld());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.tracker.show",
                t.getGeneration(),
                t.getHolderUuid() == null ? Component.translatable("command.usless_mobs.none") : Component.literal(t.getHolderName()),
                t.getHolderDimension(),
                (int) t.getHolderX(),
                (int) t.getHolderY(),
                (int) t.getHolderZ()).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int clearTracker(CommandContext<CommandSourceStack> ctx) {
        ScytheTracker t = ScytheTracker.get(ctx.getSource().getServer().overworld());
        t.clearHolder();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.tracker.cleared").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
