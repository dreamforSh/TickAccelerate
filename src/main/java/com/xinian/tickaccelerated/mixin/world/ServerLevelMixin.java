package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-level TPS compensations: random tick speed and day/night cycle.
 *
 * <h3>Random tick speed</h3>
 * <p>At 20 TPS, each chunk receives 3 × 20 = 60 random ticks/sec.
 * At 10 TPS, it only gets 3 × 10 = 30/sec — half the intended rate.
 * We scale the {@code randomTickSpeed} parameter by {@code 20 / tps}.</p>
 *
 * <h3>Day time</h3>
 * <p>Vanilla: day time advances by 1 per tick in {@code tickTime()}.
 * At low TPS the day/night cycle slows down.
 * We add extra day time so the cycle progresses at the correct real-time rate.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    /**
     * Multiply the {@code randomTickSpeed} parameter by the speed multiplier.
     */
    @ModifyVariable(method = "tickChunk", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int tickaccelerate$compensateRandomTickSpeed(int randomTickSpeed) {
        if (randomTickSpeed <= 0) return randomTickSpeed;
        try {
            if (!TickAccelerateConfig.INSTANCE.enableRandomTick.get()) return randomTickSpeed;
        } catch (Exception e) {
            return randomTickSpeed;
        }

        ServerLevel self = (ServerLevel) (Object) this;
        float multiplier = TpsHelper.getSpeedMultiplier(self.getServer());
        if (multiplier <= 1.0F) return randomTickSpeed;

        float scaled = randomTickSpeed * multiplier;
        int whole = (int) scaled;
        float fraction = scaled - whole;
        if (fraction > 0 && self.getRandom().nextFloat() < fraction) {
            whole++;
        }
        return whole;
    }

    /**
     * Compensate day/night cycle at the end of tickTime.
     * <p>Vanilla advances dayTime by 1 per tick (via {@code advanceDaytime()}).
     * At low TPS, this means the day/night cycle progresses slower.
     * We add extra dayTime so the cycle speed stays constant in real time.</p>
     */
    @Inject(method = "tickTime", at = @At("TAIL"))
    private void tickaccelerate$compensateDayTime(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;

        try {
            if (!TickAccelerateConfig.INSTANCE.enableDayTime.get()) return;
        } catch (Exception e) {
            return;
        }

        if (!self.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self.getServer());
        if (multiplier <= 1.0F) return;

        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        if (extra > 0) {
            self.setDayTime(self.getLevelData().getDayTime() + extra);
        }
    }
}
