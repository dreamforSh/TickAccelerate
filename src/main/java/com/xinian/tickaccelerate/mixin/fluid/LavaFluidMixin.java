package com.xinian.tickaccelerate.mixin.fluid;

import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.util.TPSUtil;
import com.xinian.tickaccelerate.util.TPSCalculator;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.Fluids; // Added import for Fluids
import net.minecraft.resources.ResourceLocation; // Added import for ResourceLocation
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Inject(method = "getTickDelay", at = @At("RETURN"), cancellable = true)
    private void tickRateTT20(LevelReader p_76226_, CallbackInfoReturnable<Integer> cir) {
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.fluidAcceleration()) return;
        int original = cir.getReturnValue();
        // Pass the ResourceLocation for LavaFluid
        cir.setReturnValue(TPSUtil.tt20(original, true, TPSCalculator.tpsMultiplier, Fluids.LAVA.builtInRegistryHolder().key().location()));
    }
}