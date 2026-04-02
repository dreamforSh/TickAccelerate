package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Potion effect duration TPS compensation.
 *
 * <h3>Vanilla logic (MobEffectInstance.tick)</h3>
 * <pre>
 * public boolean tick(LivingEntity entity, Runnable onUpdate) {
 *     ...
 *     this.tickDownDuration();   // duration--
 *     ...
 * }
 * </pre>
 * We subtract extra ticks from {@code duration} after vanilla's single decrement,
 * so effects expire in the same real time regardless of TPS.
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    @Shadow
    private int duration;

    @Shadow
    public abstract boolean isInfiniteDuration();

    /**
     * After vanilla decrements duration by 1, we decrement it further based on TPS.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensatePotionDuration(
            LivingEntity entity, Runnable onUpdate,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity.level().isClientSide()) return;
        if (!TickAccelerateConfig.INSTANCE.enablePotionEffect.get()) return;
        if (this.isInfiniteDuration() || this.duration <= 0) return;

        float multiplier = TpsHelper.getSpeedMultiplier(entity);
        int extra = TpsHelper.computeExtraTicks(multiplier, entity.getRandom().nextFloat());
        if (extra > 0) {
            this.duration = Math.max(0, this.duration - extra);
        }
    }
}

