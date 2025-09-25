package com.xinian.tickaccelerate.command;

import net.minecraftforge.common.MinecraftForge;

public class CommandRegistry {

    public static void registerCommands() {
        MinecraftForge.EVENT_BUS.addListener(MainCommand::register);
    }
}
