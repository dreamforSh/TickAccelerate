package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arrow/projectile life timer TPS compensation.
 *
 * <h3>Vanilla logic (AbstractArrow.tick)</h3>
 * <pre>
 * // When embedded in a block:
 * this.life++;
 * if (this.life >= 1200) {
 *     this.discard();
 * }
 * </pre>
 * <p>At low TPS, arrows stuck in blocks persist longer in real time.
 * We increment the life counter by extra ticks so arrows despawn
 * in the correct real time (60 seconds).</p>
 */
@Mixin(AbstractArrow.class)
public abstract class ArrowMixin {

    @Shadow
    private int life;

    @Unique
    private final float[] tickaccelerate$lifeAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateArrowLife(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.level().isClientSide()) return;
        if (this.life <= 0) return;
        try {
            if (!TickAccelerateConfig.INSTANCE.enableArrowLife.get()) return;
        } catch (Exception e) { return; }

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$lifeAccum);
        if (extra > 0) {
            this.life += extra;
        }
    }
}

