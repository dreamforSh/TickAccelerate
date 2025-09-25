package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.config.MainConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class ToggleCommand {

    public static CompletableFuture<Suggestions> suggestConfigKeys(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MainConfig.getBooleanOptions().stream().map(option -> option.getKey()).forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static int executeToggle(CommandContext<CommandSourceStack> context) {
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
}
