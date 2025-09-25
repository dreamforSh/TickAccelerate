package com.xinian.tickaccelerate.mixin;

import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.server.dedicated.ServerWatchdog;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWatchdog.class)
public abstract class ServerWatchdogMixin {

    @Shadow @Final private long maxTickTime;

    @Redirect(method = "run", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/ServerWatchdog;maxTickTime:J", opcode = Opcodes.GETFIELD))
    private long cancelServerWatchdog(ServerWatchdog instance) {
        return TickAccelerate.config.disableServerWatchdog() ? Long.MAX_VALUE : this.maxTickTime;
    }
}