package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Potion effect duration TPS compensation.
 * <p>Each tick decreases effect duration by {@code 20 / tps} instead of 1,
 * so effects expire in the same real time regardless of server TPS.</p>
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    @Shadow
    private int duration;

    @Shadow
    public abstract boolean isInfiniteDuration();

    /**
     * After {@code tickDownDuration} reduces duration by 1, apply extra reductions.
     * Only applies when the effect is ticking on a {@link ServerPlayer}.
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void tickaccelerate$compensateEffectDuration(LivingEntity entity, Runnable onExpiry, CallbackInfoReturnable<Boolean> cir) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof ServerPlayer)) return;
        if (!TickAccelerateConfig.INSTANCE.enablePotionEffect.get()) return;
        if (this.isInfiniteDuration() || this.duration <= 0) return;

        float multiplier = TpsHelper.getSpeedMultiplier(entity);
        if (multiplier <= 1.0F) return;

        int extraTicks = (int) (multiplier - 1.0F);
        float fraction = multiplier - 1.0F - extraTicks;
        if (fraction > 0 && entity.getRandom().nextFloat() < fraction) {
            extraTicks++;
        }
        if (extraTicks > 0) {
            this.duration = Math.max(0, this.duration - extraTicks);
        }
    }
}

