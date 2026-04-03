package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lava spread speed TPS compensation.
 *
 * <p>{@code LavaFluid} overrides {@code getSpreadDelay} (the base injection on
 * {@code FlowingFluid} does not apply here), so we inject separately.</p>
 *
 * <h3>Vanilla logic (LavaFluid.getSpreadDelay)</h3>
 * <pre>
 * int i = this.getTickDelay(level); // 10 (nether) or 30 (overworld)
 * // possibly ×4 when flowing uphill
 * return i;
 * </pre>
 *
 * <h3>Compensation strategy</h3>
 * <p>The spread delay is scaled by {@code tps / 20} (tickFactor) so that
 * the real-time interval between flow steps stays constant regardless of TPS.
 * Stochastic rounding is used: the fractional part of the scaled delay
 * becomes the probability of rounding down instead of up.  Over many ticks
 * this yields the exact correct average delay, eliminating the systematic
 * bias that plain {@code Math.round} introduces on long lava delays (30t).</p>
 */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Inject(method = "getSpreadDelay", at = @At("RETURN"), cancellable = true)
    private void tickaccelerate$scaleLavaSpreadDelay(
            Level level, BlockPos pos, FluidState currentState, FluidState newState,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (level.isClientSide()) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableFluidSpeed) return;

        float tickFactor = TpsHelper.getTickFactor(server);
        if (tickFactor >= 1.0F) return;

        int original = cir.getReturnValue();
        float scaled = original * tickFactor;
        int floor = (int) scaled;
        float frac = scaled - floor;
        int result = (frac > 0 && level.getRandom().nextFloat() < frac) ? floor + 1 : floor;
        cir.setReturnValue(Math.max(1, result));
    }
}
