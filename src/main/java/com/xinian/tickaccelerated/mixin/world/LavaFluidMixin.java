package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
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
 */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Inject(method = "getSpreadDelay", at = @At("RETURN"), cancellable = true)
    private void tickaccelerate$scaleLavaSpreadDelay(
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

