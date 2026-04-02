package com.xinian.tickaccelerated;

import com.mojang.logging.LogUtils;
import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Tick Accelerate mod entry point.
 * <p>Registers the server-side configuration on startup.
 * All TPS compensation is handled through Mixin classes.</p>
 */
@Mod(TickAccelerate.MODID)
public class TickAccelerate {

    public static final String MODID = "tickaccelerate";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TickAccelerate(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, TickAccelerateConfig.SPEC);
        LOGGER.info("Tick Accelerate initialized – TPS compensation via Mixin active");
    }
}

