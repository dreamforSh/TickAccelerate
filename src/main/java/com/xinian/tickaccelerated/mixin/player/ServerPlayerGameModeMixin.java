package com.xinian.tickaccelerated.mixin.player;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block breaking TPS compensation via {@code gameTicks} acceleration.
 *
 * <h3>Approach</h3>
 * <p>All block-breaking timing in {@link ServerPlayerGameMode} is driven by
 * {@code gameTicks}: elapsed ticks = {@code gameTicks - startTick}.</p>
 * <p>By advancing {@code gameTicks} faster at low TPS, both the incremental
 * progress path ({@code incrementDestroyProgress}) and the
 * {@code STOP_DESTROY_BLOCK} validation path are automatically compensated.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerPlayer player;

    @Shadow
    private int gameTicks;

    @Unique
    private final float[] tickaccelerate$accum = new float[1];

    /**
     * Advances {@code gameTicks} by extra ticks before vanilla's own {@code gameTicks++}.
     * Uses a deterministic accumulator to avoid stochastic jitter in break progress.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void tickaccelerate$compensateGameTicks(CallbackInfo ci) {
        if (!TickAccelerateConfig.INSTANCE.enableBlockBreaking.get()) return;
        float multiplier = TpsHelper.getSpeedMultiplier(this.player);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$accum);
        if (extra > 0) {
            this.gameTicks += extra;
        }
    }
}
