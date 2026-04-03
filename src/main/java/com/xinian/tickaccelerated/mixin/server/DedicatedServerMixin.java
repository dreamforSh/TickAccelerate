package com.xinian.tickaccelerated.mixin.server;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables the vanilla server watchdog by returning {@code 0L}
 * from {@code getMaxTickLength}.
 * <p>Vanilla checks {@code if (getMaxTickLength() > 0L)} before creating the watchdog thread,
 * so returning 0 prevents it from ever starting.</p>
 * <p>Returning {@code Long.MAX_VALUE} would overflow when multiplied by
 * {@code NANOSECONDS_PER_MILLISECOND} in the {@code ServerWatchdog} constructor,
 * producing a negative value that triggers an immediate crash.</p>
 * <p>The config may not be loaded during {@code initServer()} (SERVER configs load
 * when a world is loaded), so we default to disabling the watchdog if the config
 * is unavailable.</p>
 */
@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {

    @Inject(method = "getMaxTickLength", at = @At("HEAD"), cancellable = true)
    private void tickaccelerate$disableWatchdog(CallbackInfoReturnable<Long> cir) {
        boolean disable;
        try {
            ConfigSnapshot config = ConfigSnapshot.get((MinecraftServer) (Object) this);
            disable = config.disableWatchdog;
        } catch (Exception e) {
            disable = true;
        }
        if (disable) {
            cir.setReturnValue(0L);
        }
    }
}
