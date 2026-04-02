package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntity TPS compensation for hurtTime, deathTime, airSupply, and item-use duration.
 *
 * <h3>hurtTime</h3>
 * <p>Vanilla: {@code this.hurtTime--} per tick (in baseTick).
 * We decrement extra ticks so the damage flash duration stays constant in real time.</p>
 *
 * <h3>deathTime</h3>
 * <p>Vanilla: {@code this.deathTime++} per tick (in tickDeath).
 * We increment extra ticks so the death animation completes in the same real time.</p>
 *
 * <h3>airSupply</h3>
 * <p>Vanilla: air increases by 4 per tick when surfacing.
 * We add extra air recovery so breath restores at the same real-time rate.</p>
 *
 * <h3>itemUse</h3>
 * <p>Vanilla: {@code --this.useItemRemaining} per tick in updateUsingItem.
 * We decrement extra so eating, drinking, bow charging, etc. takes the same real time.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public int hurtTime;

    @Shadow
    public int deathTime;


    @Shadow
    public int swingTime;

    @Shadow
    public boolean swinging;

    @Shadow
    protected int useItemRemaining;

    @Shadow
    protected int attackStrengthTicker;

    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    public abstract int getCurrentSwingDuration();

    /**
     * Compensate hurtTime, airSupply, and attackStrengthTicker at the end of baseTick.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void tickaccelerate$compensateBaseTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        if (multiplier <= 1.0F) return;

        // ── hurtTime compensation ──
        if (TickAccelerateConfig.INSTANCE.enableHurtTime.get() && this.hurtTime > 0) {
            int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
            if (extra > 0) {
                this.hurtTime = Math.max(0, this.hurtTime - extra);
            }
        }

        // ── airSupply compensation ──
        if (TickAccelerateConfig.INSTANCE.enableAirSupply.get()) {
            int air = self.getAirSupply();
            int maxAir = self.getMaxAirSupply();
            if (air < maxAir && air > 0) {
                int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
                if (extra > 0) {
                    int newAir = Math.min(air + 4 * extra, maxAir);
                    self.setAirSupply(newAir);
                }
            }
        }

        // ── attackStrengthTicker compensation (players only) ──
        if (self instanceof ServerPlayer && TickAccelerateConfig.INSTANCE.enableAttackCooldown.get()) {
            int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
            if (extra > 0) {
                this.attackStrengthTicker += extra;
            }
        }

        // ── invulnerableTime compensation (non-player entities) ──
        if (TickAccelerateConfig.INSTANCE.enableInvulnerability.get()
                && !(self instanceof ServerPlayer)
                && self.invulnerableTime > 0) {
            int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
            if (extra > 0) {
                self.invulnerableTime = Math.max(0, self.invulnerableTime - extra);
            }
        }
    }

    /**
     * Compensate swingTime at the end of updateSwingTime.
     * <p>Vanilla: {@code this.swingTime++} per tick. We increment extra ticks
     * so the attack arm swing animation completes at the correct real-time speed.</p>
     */
    @Inject(method = "updateSwingTime", at = @At("TAIL"))
    private void tickaccelerate$compensateSwingTime(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!TickAccelerateConfig.INSTANCE.enableSwingSpeed.get()) return;
        if (!this.swinging || this.swingTime <= 0) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        if (multiplier <= 1.0F) return;

        int duration = this.getCurrentSwingDuration();
        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        if (extra > 0) {
            this.swingTime += extra;
            if (this.swingTime >= duration) {
                this.swingTime = 0;
                this.swinging = false;
            }
        }
    }

    /**
     * Compensate deathTime at the end of tickDeath.
     */
    @Inject(method = "tickDeath", at = @At("TAIL"))
    private void tickaccelerate$compensateDeathTime(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!TickAccelerateConfig.INSTANCE.enableDeathTime.get()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        if (extra > 0) {
            this.deathTime += extra;
        }
    }

    /**
     * Compensate item-use duration at HEAD of updateUsingItem.
     * <p>By decrementing extra ticks before vanilla's own {@code --this.useItemRemaining},
     * the completion check fires at the correct real time.</p>
     */
    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void tickaccelerate$compensateItemUse(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!(self instanceof ServerPlayer)) return;
        if (!TickAccelerateConfig.INSTANCE.enableItemUse.get()) return;
        if (!this.isUsingItem() || this.useItemRemaining <= 1) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        if (extra > 0) {
            // Don't go below 1 – let vanilla's own decrement handle the final tick
            this.useItemRemaining = Math.max(1, this.useItemRemaining - extra);
        }
    }
}

