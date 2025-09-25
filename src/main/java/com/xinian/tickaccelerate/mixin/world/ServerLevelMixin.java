package com.xinian.tickaccelerate.mixin.world;

import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Redirect(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/WritableLevelData;getDayTime()J"))
    private long addMissingTicksToTime(WritableLevelData instance) {
        long original = instance.getDayTime();
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.timeAcceleration()) return original;

        int ticksToApply = TickAccelerate.TPS_CALCULATOR.applicableMissedTicks();
        int cap = TickAccelerate.TPS_CALCULATOR.getEffectiveTickCap();
        if (cap > 0) {
            ticksToApply = Math.min(ticksToApply, cap);
        }

        return original + ticksToApply;
    }
}