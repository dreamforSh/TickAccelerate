package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntity TPS compensation for hurtTime, deathTime, airSupply,
 * item-use duration, swing speed, and invulnerability frames.
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

    @Unique
    private final float[] tickaccelerate$hurtAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$airAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$iFrameAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$swingAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$deathAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$useAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$attackAccum = new float[1];

    /**
     * Compensate hurtTime, airSupply, and invulnerableTime at the end of baseTick.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void tickaccelerate$compensateBaseTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        if (multiplier <= 1.0F) return;

        if (TickAccelerateConfig.INSTANCE.enableHurtTime.get() && this.hurtTime > 0) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$hurtAccum);
            if (extra > 0) {
                this.hurtTime = Math.max(0, this.hurtTime - extra);
            }
        }

        if (TickAccelerateConfig.INSTANCE.enableAirSupply.get()) {
            int air = self.getAirSupply();
            int maxAir = self.getMaxAirSupply();
            if (air < maxAir && air > 0) {
                int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$airAccum);
                if (extra > 0) {
                    self.setAirSupply(Math.min(air + 4 * extra, maxAir));
                }
            }
        }

        if (TickAccelerateConfig.INSTANCE.enableInvulnerability.get()
                && !(self instanceof ServerPlayer)
                && self.invulnerableTime > 0) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$iFrameAccum);
            if (extra > 0) {
                self.invulnerableTime = Math.max(0, self.invulnerableTime - extra);
            }
        }

        if (self instanceof ServerPlayer && TickAccelerateConfig.INSTANCE.enableAttackCooldown.get()) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$attackAccum);
            if (extra > 0) {
                this.attackStrengthTicker += extra;
            }
        }
    }

    /**
     * Compensate swingTime so attack arm swing animation plays at correct real-time speed.
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
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$swingAccum);
        if (extra > 0) {
            this.swingTime += extra;
            if (this.swingTime >= duration) {
                this.swingTime = 0;
                this.swinging = false;
            }
        }
    }

    /**
     * Compensate deathTime so the death animation completes in the same real time.
     */
    @Inject(method = "tickDeath", at = @At("TAIL"))
    private void tickaccelerate$compensateDeathTime(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!TickAccelerateConfig.INSTANCE.enableDeathTime.get()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$deathAccum);
        if (extra > 0) {
            this.deathTime += extra;
        }
    }

    /**
     * Compensate item-use duration (eating, drinking, bow charge, etc).
     */
    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void tickaccelerate$compensateItemUse(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!(self instanceof ServerPlayer)) return;
        if (!TickAccelerateConfig.INSTANCE.enableItemUse.get()) return;
        if (!this.isUsingItem() || this.useItemRemaining <= 1) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$useAccum);
        if (extra > 0) {
            this.useItemRemaining = Math.max(1, this.useItemRemaining - extra);
        }
    }
}

