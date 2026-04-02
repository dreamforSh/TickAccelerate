package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nether portal transition time TPS compensation.
 *
 * <h3>Vanilla logic (PortalProcessor.processPortalTeleportation)</h3>
 * <pre>
 * if (!this.insidePortalThisTick) { this.decayTick(); return false; }
 * else { this.insidePortalThisTick = false;
 *        return canChangeDimensions && this.portalTime++ >= threshold; }
 * </pre>
 * <p>We add extra increments to {@code portalTime} before vanilla's check.</p>
 */
@Mixin(PortalProcessor.class)
public abstract class PortalProcessorMixin {

    @Shadow
    private int portalTime;

    @Shadow
    private boolean insidePortalThisTick;

    @Unique
    private final float[] tickaccelerate$portalAccum = new float[1];

    /**
     * Add extra portalTime increments at HEAD of processPortalTeleportation.
     * Vanilla will then do its own {@code portalTime++} and the threshold check.
     */
    @Inject(method = "processPortalTeleportation", at = @At("HEAD"))
    private void tickaccelerate$compensatePortalTime(
            ServerLevel level, Entity entity, boolean canChangeDimensions,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!this.insidePortalThisTick) return;
        if (!TickAccelerateConfig.INSTANCE.enablePortalTime.get()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(entity);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$portalAccum);
        if (extra > 0) {
            this.portalTime += extra;
        }
    }
}
