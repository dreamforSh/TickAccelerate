package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.config.MainConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

public class MainCommand {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tickaccelerate").executes(MainCommand::executeMain)
                        .then(Commands.literal("tps").executes(TpsCommand::executeTps))
                        .then(Commands.literal("status").executes(StatusCommand::executeStatus))
                        .then(Commands.literal("toggle").requires(ctx -> ctx.hasPermission(3))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(ToggleCommand::suggestConfigKeys)
                                        .executes(ToggleCommand::executeToggle)))
                        .then(Commands.literal("reload").requires(ctx -> ctx.hasPermission(2)).executes(ReloadCommand::executeReload))
                        .then(Commands.literal("help").executes(HelpCommand::executeHelp))
        );
    }

    private static int executeMain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.main.version", TickAccelerate.VERSION));
        Component enabledText = Component.translatable(TickAccelerate.config.enabled() ? "tickaccelerate.status.on" : "tickaccelerate.status.off");
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.main.enabled", enabledText));
        return 1;
    }
}
