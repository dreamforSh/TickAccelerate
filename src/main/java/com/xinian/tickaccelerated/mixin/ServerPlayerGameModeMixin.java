package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block breaking TPS compensation.
 * <p>Multiplies destroy progress by {@code 20 / tps} so blocks break in the same real time.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerPlayer player;

    /**
     * Compensates the return value of {@code incrementDestroyProgress} (tick path).
     */
    @Inject(method = "incrementDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void tickaccelerate$compensateDestroyProgress(BlockState state, BlockPos pos, int startTick, CallbackInfoReturnable<Float> cir) {
        if (!TickAccelerateConfig.INSTANCE.enableBlockBreaking.get()) return;
        float multiplier = TpsHelper.getSpeedMultiplier(this.player);
        if (multiplier > 1.0F) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }

    /**
     * Compensates the {@code STOP_DESTROY_BLOCK} validation check (2nd getDestroyProgress call).
     */
    @Redirect(
        method = "handleBlockBreakAction",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F",
                 ordinal = 1)
    )
    private float tickaccelerate$compensateStopDestroyCheck(BlockState state, Player playerArg, Level level, BlockPos pos) {
        float original = state.getDestroyProgress(playerArg, level, pos);
        if (!TickAccelerateConfig.INSTANCE.enableBlockBreaking.get()) return original;
        return original * TpsHelper.getSpeedMultiplier(this.player);
    }
}
