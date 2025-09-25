package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class HelpCommand {

    public static int executeHelp(CommandContext<CommandSourceStack> context) {
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
