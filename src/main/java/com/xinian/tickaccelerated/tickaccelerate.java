package com.xinian.tickaccelerated;

import com.mojang.logging.LogUtils;
import com.xinian.tickaccelerated.command.TickAccelerateCommand;
import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.ServerI18n;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/**
 * Tick Accelerate mod entry point.
 * <p>Registers the server-side configuration and commands on startup.
 * All TPS compensation is handled through Mixin classes.</p>
 */
@Mod(TickAccelerate.MODID)
public class TickAccelerate {

    public static final String MODID = "tickaccelerate";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TickAccelerate(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, TickAccelerateConfig.SPEC);
        modEventBus.addListener(this::onConfigLoad);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        ServerI18n.load("en_us");
        LOGGER.info("Tick Accelerate initialized – TPS compensation via Mixin active");
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            try {
                ServerI18n.load(TickAccelerateConfig.INSTANCE.serverLocale.get());
            } catch (Exception e) {
                ServerI18n.load("en_us");
            }
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TickAccelerateCommand.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        TpsHelper.reset();
    }
}
