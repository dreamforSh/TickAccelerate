package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
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
        try {
            if (!TickAccelerateConfig.INSTANCE.enableFluidSpeed.get()) return;
        } catch (Exception e) {
            return;
        }

        MinecraftServer server = level.getServer();
        if (server == null) return;

        float tickFactor = TpsHelper.getTickFactor(server);
        if (tickFactor >= 1.0F) return;

        int original = cir.getReturnValue();
        int scaled = Math.max(1, Math.round(original * tickFactor));
        cir.setReturnValue(scaled);
    }
}
