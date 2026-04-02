package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Random tick speed TPS compensation.
 *
 * <h3>Vanilla logic (ServerChunkCache → ServerLevel.tickChunk)</h3>
 * <pre>
 * int k = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING); // default 3
 * this.level.tickChunk(levelchunk, k);
 * </pre>
 * <p>At 20 TPS, each chunk receives 3 × 20 = 60 random ticks/sec.
 * At 10 TPS, it only gets 3 × 10 = 30/sec — half the intended rate.</p>
 * <p>We scale the {@code randomTickSpeed} parameter by {@code 20 / tps}
 * (stochastic rounding for the fractional part) so the effective
 * random-tick rate in real time stays constant.</p>
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
}
