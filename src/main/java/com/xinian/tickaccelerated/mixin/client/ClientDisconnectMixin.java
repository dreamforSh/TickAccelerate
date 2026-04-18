package com.xinian.tickaccelerated.mixin.client;

import com.xinian.tickaccelerated.util.ClientTpsMonitor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resets {@link ClientTpsMonitor} when disconnecting from a server.
 * <p>Without this, stale TPS data from a previous session would bleed
 * into the next connection, causing incorrect compensation values
 * for the first few seconds after joining a new server.</p>
 */
@Mixin(Minecraft.class)
public abstract class ClientDisconnectMixin {

    @Inject(method = "disconnect()V", at = @At("HEAD"))
    private void tickaccelerate$resetClientTps(CallbackInfo ci) {
        ClientTpsMonitor.reset();
    }
}

