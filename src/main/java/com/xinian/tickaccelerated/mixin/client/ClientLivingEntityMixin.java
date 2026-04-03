package com.xinian.tickaccelerated.mixin.client;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.ClientTpsMonitor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side animation TPS compensation.
 *
 * <p>When the server runs at low TPS, the client also ticks slower because
 * entity ticks are driven by server sync. This causes tick-based animations
 * (hurt flash, death animation, swing, invulnerability) to play slower
 * than intended.</p>
 *
 * <h3>Compensated animations</h3>
 * <ul>
 *   <li><b>hurtTime</b> — hurt flash / tilt. Vanilla: {@code hurtTime--} per tick</li>
 *   <li><b>deathTime</b> — death fall-over animation. Vanilla: {@code deathTime++} per tick</li>
 *   <li><b>swingTime</b> — attack arm swing. Vanilla: {@code swingTime++} in updateSwingTime</li>
 *   <li><b>invulnerableTime</b> — damage i-frames. Vanilla: {@code invulnerableTime--} per tick</li>
 * </ul>
 *
 * <p>These only apply when {@code level().isClientSide()} is true,
 * and use {@link ClientTpsMonitor} for timing rather than server MSPT.</p>
 */
@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin {

    @Shadow public int hurtTime;
    @Shadow public int deathTime;
    @Shadow public int swingTime;
    @Shadow public boolean swinging;

    @Shadow public abstract int getCurrentSwingDuration();

    @Unique private final float[] tickaccelerate$clientHurtAccum = new float[1];
    @Unique private final float[] tickaccelerate$clientIFrameAccum = new float[1];
    @Unique private final float[] tickaccelerate$clientDeathAccum = new float[1];
    @Unique private final float[] tickaccelerate$clientSwingAccum = new float[1];

    /**
     * After vanilla's {@code baseTick()}, apply extra hurtTime / invulnerableTime
     * decrements on the CLIENT side.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void tickaccelerate$clientCompensateBaseTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        try {
            if (!TickAccelerateConfig.INSTANCE.enableClientAnimations.get()) return;
        } catch (Exception e) {
            return;
        }

        float mult = ClientTpsMonitor.getSpeedMultiplier();
        if (mult <= 1.0F) return;

        // ── hurtTime ──
        if (this.hurtTime > 0) {
            int extra = ClientTpsMonitor.computeExtraTicksDeterministic(this.tickaccelerate$clientHurtAccum);
            if (extra > 0) {
                this.hurtTime = Math.max(0, this.hurtTime - extra);
            }
        }

        // ── invulnerableTime ──
        if (self.invulnerableTime > 0) {
            int extra = ClientTpsMonitor.computeExtraTicksDeterministic(this.tickaccelerate$clientIFrameAccum);
            if (extra > 0) {
                self.invulnerableTime = Math.max(0, self.invulnerableTime - extra);
            }
        }
    }

    /**
     * After vanilla's {@code tickDeath()}, apply extra deathTime increments
     * on the CLIENT side.
     */
    @Inject(method = "tickDeath", at = @At("TAIL"))
    private void tickaccelerate$clientCompensateDeathTime(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        try {
            if (!TickAccelerateConfig.INSTANCE.enableClientAnimations.get()) return;
        } catch (Exception e) {
            return;
        }

        int extra = ClientTpsMonitor.computeExtraTicksDeterministic(this.tickaccelerate$clientDeathAccum);
        if (extra > 0) {
            this.deathTime += extra;
        }
    }

    /**
     * After vanilla's {@code updateSwingTime()}, apply extra swingTime increments
     * on the CLIENT side so the arm swing animation plays at correct real-time speed.
     */
    @Inject(method = "updateSwingTime", at = @At("TAIL"))
    private void tickaccelerate$clientCompensateSwingTime(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        try {
            if (!TickAccelerateConfig.INSTANCE.enableClientAnimations.get()) return;
        } catch (Exception e) {
            return;
        }

        if (!this.swinging || this.swingTime <= 0) return;

        int duration = this.getCurrentSwingDuration();
        int extra = ClientTpsMonitor.computeExtraTicksDeterministic(this.tickaccelerate$clientSwingAccum);
        if (extra > 0) {
            this.swingTime += extra;
            // Let vanilla's next tick handle the completion check
            if (this.swingTime >= duration) {
                this.swingTime = 0;
                this.swinging = false;
            }
        }

        // Recompute attackAnim to keep it in sync
        // (vanilla formula: attackAnim = swingTime / duration)
    }
}

