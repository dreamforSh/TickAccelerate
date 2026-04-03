package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fluid spread speed TPS compensation.
 *
 * <h3>Vanilla logic</h3>
 * <pre>
 * // FlowingFluid.getSpreadDelay (base implementation):
 * protected int getSpreadDelay(Level level, BlockPos pos, FluidState cur, FluidState next) {
 *     return this.getTickDelay(level); // Water=5, Lava=10/30
 * }
 * </pre>
 * <p>The spread delay is a tick count. At low TPS each tick takes more real time,
 * so fluids flow slower. We scale the delay by {@code tps / 20} (tickFactor)
 * so the real-time spread speed stays constant.</p>
 *
 * <p>Stochastic rounding is used: the fractional part of the scaled delay
 * becomes the probability of rounding up, giving an exact correct average
 * delay and avoiding systematic bias from plain {@code Math.round}.</p>
 *
 * <p>Note: {@code LavaFluid} overrides this method, so a separate mixin handles lava.</p>
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "getSpreadDelay", at = @At("RETURN"), cancellable = true)
    private void tickaccelerate$scaleFluidSpreadDelay(
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
        // Stochastic rounding for correct average delay
        int floor = (int) scaled;
        float frac = scaled - floor;
        int result = (frac > 0 && level.getRandom().nextFloat() < frac) ? floor + 1 : floor;
        cir.setReturnValue(Math.max(1, result));
    }
}
