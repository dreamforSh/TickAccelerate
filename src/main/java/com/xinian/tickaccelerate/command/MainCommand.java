package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.config.MainConfig;
import com.xinian.tickaccelerate.util.TPSUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainCommand {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tickaccelerate").executes(MainCommand::executeMain)
                        .then(Commands.literal("tps").executes(MainCommand::executeTps))
                        .then(Commands.literal("status").executes(MainCommand::executeStatus))
                        .then(Commands.literal("toggle").requires(ctx -> ctx.hasPermission(3))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(MainCommand::suggestConfigKeys)
                                        .executes(MainCommand::executeToggle)))
                        .then(Commands.literal("reload").requires(ctx -> ctx.hasPermission(2)).executes(MainCommand::executeReload))
                        .then(Commands.literal("help").executes(MainCommand::executeHelp))
        );
    }

    private static CompletableFuture<Suggestions> suggestConfigKeys(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MainConfig.getBooleanOptions().stream().map(option -> option.getKey()).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int executeMain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.main.version", TickAccelerate.VERSION));
        Component enabledText = Component.translatable(TickAccelerate.config.enabled() ? "tickaccelerate.status.on" : "tickaccelerate.status.off");
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.main.enabled", enabledText));
        return 1;
    }

    private static int executeTps(CommandContext<CommandSourceStack> context) {
        return MainCommand.executeTps(context, true);
    }

    private static int executeTps(CommandContext<CommandSourceStack> context, boolean missedTicks) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.tps.line",
                TPSUtil.colorizeTPS(TickAccelerate.TPS_CALCULATOR.getTPS(), true),
                TPSUtil.colorizeTPS(TickAccelerate.TPS_CALCULATOR.getAverageTPS(), true),
                TPSUtil.colorizeTPS(TickAccelerate.TPS_CALCULATOR.getMostAccurateTPS(), true)
        ));

        if (missedTicks) {
            source.sendSystemMessage(Component.translatable("tickaccelerate.command.tps.missed",
                    TPSUtil.formatMissedTicks(TickAccelerate.TPS_CALCULATOR.getAllMissedTicks())
            ));
        }
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.header"));

        for (var option : MainConfig.getBooleanOptions()) {
            String key = option.getKey();
            boolean value = TickAccelerate.config.getBoolean(key);

            String[] words = key.replace('-', ' ').replace('.', ' ').split(" ");
            List<String> capitalizedWords = new ArrayList<>();
            for (String word : words) {
                if (word.length() > 0) {
                    capitalizedWords.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
                }
            }
            String formattedName = String.join(" ", capitalizedWords);
            Component statusText = Component.translatable(value ? "tickaccelerate.status.on" : "tickaccelerate.status.off");

            source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.line", formattedName, statusText));
        }

        source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.footer"));

        executeTps(context, false);
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.version", TickAccelerate.VERSION));
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.mspt", TickAccelerate.TPS_CALCULATOR.getMSPT()));
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.tps.missed", TPSUtil.formatMissedTicks(TickAccelerate.TPS_CALCULATOR.getAllMissedTicks())));

        return 1;
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = StringArgumentType.getString(context, "key");

        if (MainConfig.getBooleanOptions().stream().noneMatch(o -> o.getKey().equals(key))) {
            source.sendFailure(Component.translatable("tickaccelerate.command.toggle.unknown_key", key));
            return 0;
        }

        boolean currentValue = TickAccelerate.config.getBoolean(key);
        boolean newValue = !currentValue;
        TickAccelerate.config.setBoolean(key, newValue);
        TickAccelerate.config.save();

        Component enabledText = Component.translatable(newValue ? "tickaccelerate.status.enabled" : "tickaccelerate.status.disabled");
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.toggle.feedback", key, enabledText));
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TickAccelerate.config.reload();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.reload.config"));
        TickAccelerate.blockEntityMaskConfig.reload();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.reload.mask_config"));
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.help.header"));

        sendHelpLine(source, "", "", "tickaccelerate.command.help.cmd.main", "0");
        sendHelpLine(source, "tps", "", "tickaccelerate.command.help.cmd.tps", "0");
        sendHelpLine(source, "status", "", "tickaccelerate.command.help.cmd.status", "0");
        sendHelpLine(source, "toggle", "<key>", "tickaccelerate.command.help.cmd.toggle", "3");
        sendHelpLine(source, "reload", "", "tickaccelerate.command.help.cmd.reload", "2");
        sendHelpLine(source, "help", "", "tickaccelerate.command.help.cmd.help", "0");

        return 1;
    }

    private static void sendHelpLine(CommandSourceStack source, String subcommand, String args, String descriptionKey, String permission) {
        String command = "/tickaccelerate" + (subcommand.isEmpty() ? "" : " " + subcommand) + (args.isEmpty() ? "" : " " + args);
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.help.line",
                command,
                Component.translatable(descriptionKey),
                permission
        ));
    }
}
