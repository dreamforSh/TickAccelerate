package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.context.CommandContext;
import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadCommand {

    public static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TickAccelerate.config.reload();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.reload.config"));
        TickAccelerate.blockEntityMaskConfig.reload();
        source.sendSystemMessage(Component.translatable("tickaccelerate.command.reload.mask_config"));
        return 1;
    }
}
