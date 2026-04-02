package com.xinian.tickaccelerated.mixin.client;

import com.xinian.tickaccelerated.util.ClientTpsMonitor;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Records the time of each client level tick so that
 * {@link ClientTpsMonitor} can compute the effective client-side TPS.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickaccelerate$recordClientTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ClientTpsMonitor.onTick();
    }
}

