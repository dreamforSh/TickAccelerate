package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.context.CommandContext;
import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.config.MainConfig;
import com.xinian.tickaccelerate.util.TPSUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class StatusCommand {

    public static int executeStatus(CommandContext<CommandSourceStack> context) {
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

        TpsCommand.executeTps(context, false);
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.version", TickAccelerate.VERSION));
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.status.mspt", TickAccelerate.TPS_CALCULATOR.getMSPT()));
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.tps.missed", TPSUtil.formatMissedTicks(TickAccelerate.TPS_CALCULATOR.getAllMissedTicks())));

        return 1;
    }
}
