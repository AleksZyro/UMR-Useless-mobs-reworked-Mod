package com.Momik.usless_mobs.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

public final class UmrCommand {

    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(
                    ForgeRegistries.MOB_EFFECTS.getKeys(), builder);

    private UmrCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("umr")
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("effect")
                        .then(Commands.literal("disable")
                                .then(Commands.argument("effect", ResourceLocationArgument.id())
                                        .suggests(EFFECT_SUGGESTIONS)
                                        .executes(UmrCommand::disable)))
                        .then(Commands.literal("enable")
                                .then(Commands.argument("effect", ResourceLocationArgument.id())
                                        .suggests(EFFECT_SUGGESTIONS)
                                        .executes(UmrCommand::enable)))
                        .then(Commands.literal("list")
                                .executes(UmrCommand::list))
                        .then(Commands.literal("clear")
                                .executes(UmrCommand::clear))
                )
                .then(Commands.literal("debug")
                        .executes(UmrCommand::debug))
        );
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.usless_mobs.player_only"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.debug.attributes").withStyle(ChatFormatting.YELLOW), false);
        player.getAttributes().getSyncableAttributes().forEach(inst -> {
            if (!inst.getModifiers().isEmpty()) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(inst.getAttribute());
                ctx.getSource().sendSuccess(() -> Component.literal(
                        (id == null ? "?" : id.toString()) + " base=" + inst.getBaseValue() + " value=" + inst.getValue()
                ).withStyle(ChatFormatting.GRAY), false);
                inst.getModifiers().forEach(m -> ctx.getSource().sendSuccess(() -> Component.literal(
                        "  + " + m.getName() + " " + m.getAmount() + " op=" + m.getOperation()
                ).withStyle(ChatFormatting.DARK_GRAY), false));
            }
        });

        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.debug.effects").withStyle(ChatFormatting.YELLOW), false);
        if (player.getActiveEffects().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.none").withStyle(ChatFormatting.GRAY), false);
        } else {
            player.getActiveEffects().forEach(inst -> {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
                ctx.getSource().sendSuccess(() -> Component.literal(
                        (id == null ? "?" : id.toString()) + " amp=" + inst.getAmplifier() + " dur=" + inst.getDuration() + " icon=" + inst.showIcon()
                ).withStyle(ChatFormatting.GRAY), false);
            });
        }
        return 1;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.usless_mobs.player_only"));
            return 0;
        }

        ResourceLocation id = ResourceLocationArgument.getId(ctx, "effect");
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            ctx.getSource().sendFailure(Component.translatable("command.usless_mobs.unknown_effect", id).withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean changed = SlimePowerToggle.disableEffectId(player, id);
        Component name = Component.translatable(effect.getDescriptionId());
        if (changed) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.disabled", name).withStyle(ChatFormatting.RED), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.already_disabled", name).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.usless_mobs.player_only"));
            return 0;
        }

        ResourceLocation id = ResourceLocationArgument.getId(ctx, "effect");
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        Component name = effect != null
                ? Component.translatable(effect.getDescriptionId())
                : Component.literal(id.toString());

        boolean changed = SlimePowerToggle.enableEffectId(player, id);
        if (changed) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.enabled", name).withStyle(ChatFormatting.GREEN), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.was_not_disabled", name).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.usless_mobs.player_only"));
            return 0;
        }

        List<ResourceLocation> disabled = SlimePowerToggle.getDisabledEffectIds(player);
        if (disabled.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.no_effects_disabled").withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.disabled_effects", disabled.size()).withStyle(ChatFormatting.YELLOW), false);
        for (ResourceLocation id : disabled) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
            Component nameLine = effect != null
                    ? Component.literal(" - ").append(Component.translatable(effect.getDescriptionId())).append(" (" + id + ")")
                    : Component.literal(" - " + id);
            ctx.getSource().sendSuccess(() -> nameLine.copy().withStyle(ChatFormatting.GRAY), false);
        }
        return disabled.size();
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.usless_mobs.player_only"));
            return 0;
        }

        int count = SlimePowerToggle.getDisabledEffectIds(player).size();
        SlimePowerToggle.clearDisabledEffectIds(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.usless_mobs.reenabled", count).withStyle(ChatFormatting.GREEN), false);
        return count;
    }
}
