package com.xinian.tickaccelerated;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Tickaccelerate mod entry point.
 * <p>Registers the server-side configuration on startup.</p>
 */
@Mod(tickaccelerate.MODID)
public class tickaccelerate {

    public static final String MODID = "tickaccelerate";
    private static final Logger LOGGER = LogUtils.getLogger();

    public tickaccelerate(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, TickAccelerateConfig.SPEC);
        LOGGER.info("Tickaccelerate mod initialized - TPS compensation via Mixin active");
    }
}
