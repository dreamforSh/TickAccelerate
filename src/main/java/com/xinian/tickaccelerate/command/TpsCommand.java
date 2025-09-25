package com.xinian.tickaccelerate.command;

import com.mojang.brigadier.context.CommandContext;
import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.util.TPSUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class TpsCommand {

    public static int executeTps(CommandContext<CommandSourceStack> context) {
        return executeTps(context, true);
    }

    public static int executeTps(CommandContext<CommandSourceStack> context, boolean missedTicks) {
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
}
