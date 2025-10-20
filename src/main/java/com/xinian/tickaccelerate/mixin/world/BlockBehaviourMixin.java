package com.xinian.tickaccelerate.mixin.world;

import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.util.TPSCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void onBlockBreakingCalc(BlockState state, Player player, BlockGetter getter, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.blockBreakingAcceleration()) return;
        if (player.level().isClientSide()) return;

        float original = cir.getReturnValue();
        cir.setReturnValue(original * TPSCalculator.tpsMultiplier);
    }
}