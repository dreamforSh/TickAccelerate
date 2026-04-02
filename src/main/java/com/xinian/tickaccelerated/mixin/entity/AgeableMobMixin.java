package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Baby mob growth timer TPS compensation.
 *
 * <h3>Vanilla logic (AgeableMob.aiStep)</h3>
 * <pre>
 * if (this.isAlive()) {
 *     int i = this.getAge();
 *     if (i < 0) {
 *         this.setAge(++i);   // baby growing up
 *     } else if (i > 0) {
 *         this.setAge(--i);   // breeding cooldown
 *     }
 * }
 * </pre>
 * <p>Baby mobs have a negative age (default -24000 = 20 minutes).
 * At low TPS, growth takes longer in real time.
 * We increment the age by extra ticks so babies grow up at the correct rate.</p>
 *
 * <p>Also compensates the breeding cooldown (positive age after mating),
 * which determines when the animal can breed again.</p>
 */
@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin {

    @Shadow
    protected int age;

    @Unique
    private final float[] tickaccelerate$growthAccum = new float[1];

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void tickaccelerate$compensateGrowth(CallbackInfo ci) {
        AgeableMob self = (AgeableMob) (Object) this;
        if (self.level().isClientSide()) return;
        if (!self.isAlive()) return;
        try {
            if (!TickAccelerateConfig.INSTANCE.enableMobGrowth.get()) return;
        } catch (Exception e) { return; }

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$growthAccum);
        if (extra <= 0) return;

        if (this.age < 0) {
            int newAge = this.age + extra;
            self.setAge(Math.min(0, newAge));
        } else if (this.age > 0) {
            int newAge = this.age - extra;
            self.setAge(Math.max(0, newAge));
        }
    }
}

