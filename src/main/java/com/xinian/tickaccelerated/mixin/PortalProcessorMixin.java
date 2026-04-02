package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nether portal transition time TPS compensation.
 * <p>Each tick increments {@code portalTime} by {@code 20 / tps} instead of 1,
 * so players enter the portal in the same real time regardless of TPS.</p>
 */
@Mixin(PortalProcessor.class)
public abstract class PortalProcessorMixin {

    @Shadow
    private int portalTime;

    /**
     * After {@code processPortalTeleportation} runs (which does {@code portalTime++}),
     * apply extra increments for TPS compensation. Only affects players.
     */
    @Inject(method = "processPortalTeleportation", at = @At("RETURN"))
    private void tickaccelerate$compensatePortalTime(ServerLevel level, Entity entity, boolean canChangeDimensions, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player)) return;
        if (!TickAccelerateConfig.INSTANCE.enablePortalTime.get()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(entity);
        if (multiplier <= 1.0F) return;

        int extraTicks = (int) (multiplier - 1.0F);
        float fraction = multiplier - 1.0F - extraTicks;
        if (fraction > 0 && entity.level().getRandom().nextFloat() < fraction) {
            extraTicks++;
        }
        if (extraTicks > 0 && this.portalTime > 0) {
            this.portalTime += extraTicks;
        }
    }
}

