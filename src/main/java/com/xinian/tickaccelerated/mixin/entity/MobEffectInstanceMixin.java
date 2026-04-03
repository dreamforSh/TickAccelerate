package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Potion effect duration TPS compensation.
 *
 * <h3>Vanilla logic (MobEffectInstance.tick)</h3>
 * <pre>
 * this.tickDownDuration();   // duration--
 * </pre>
 * <p>We subtract extra ticks from {@code duration} so effects expire
 * in the same real time regardless of TPS.</p>
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    @Shadow
    private int duration;

    @Shadow
    public abstract boolean isInfiniteDuration();

    @Unique
    private final float[] tickaccelerate$potionAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensatePotionDuration(
            LivingEntity entity, Runnable onUpdate,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity.level().isClientSide()) return;
        if (this.isInfiniteDuration() || this.duration <= 0) return;

        MinecraftServer server = entity.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enablePotionEffect) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$potionAccum);
        if (extra > 0) {
            this.duration = Math.max(0, this.duration - extra);
        }
    }
}
