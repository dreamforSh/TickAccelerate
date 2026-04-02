package com.xinian.tickaccelerated;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(tickaccelerate.MODID)
public class tickaccelerate {
    public static final String MODID = "tickaccelerate";
    private static final Logger LOGGER = LogUtils.getLogger();

    public tickaccelerate(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Tickaccelerate mod initialized");
    }
}
