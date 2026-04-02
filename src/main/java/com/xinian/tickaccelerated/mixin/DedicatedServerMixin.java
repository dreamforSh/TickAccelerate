package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables the vanilla server watchdog by returning {@code Long.MAX_VALUE} from {@code getMaxTickLength}.
 * <p>When TPS is intentionally low, the watchdog would otherwise crash the server.</p>
 */
@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {

    /**
     * Overrides {@code getMaxTickLength} to effectively disable the watchdog timer.
     */
    @Inject(method = "getMaxTickLength", at = @At("HEAD"), cancellable = true)
    private void tickaccelerate$disableWatchdog(CallbackInfoReturnable<Long> cir) {
        if (TickAccelerateConfig.INSTANCE.disableWatchdog.get()) {
            cir.setReturnValue(Long.MAX_VALUE);
        }
    }
}

