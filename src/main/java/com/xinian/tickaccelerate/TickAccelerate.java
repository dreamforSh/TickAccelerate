package com.xinian.tickaccelerate;

import com.xinian.tickaccelerate.command.CommandRegistry;
import com.xinian.tickaccelerate.config.BlockEntityMaskConfig;
import com.xinian.tickaccelerate.config.MainConfig;
import com.xinian.tickaccelerate.util.TPSCalculator;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Mod(TickAccelerate.MOD_ID)
public class TickAccelerate {

    public static final String MOD_ID = "tickaccelerate";
    public static final Logger LOGGER = LoggerFactory.getLogger(TickAccelerate.MOD_ID);
    public static final String VERSION = "0.7.4";
    public static final TPSCalculator TPS_CALCULATOR = new TPSCalculator();
    public static final MainConfig config = new MainConfig();
    public static final BlockEntityMaskConfig blockEntityMaskConfig = new BlockEntityMaskConfig();
    public static boolean warned = false;

    public TickAccelerate() {
        LOGGER.info("Starting TickAccelerate...");
        CompletableFuture.runAsync(() -> {
            try {
                
            } catch (RuntimeException e) {
                LOGGER.error("Failed to check for updates.");
                e.printStackTrace();
            }

        });
        CommandRegistry.registerCommands();
    }
}