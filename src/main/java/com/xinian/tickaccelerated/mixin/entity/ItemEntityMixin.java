package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dropped-item pickup delay TPS compensation.
 *
 * <h3>Vanilla logic (ItemEntity.tick)</h3>
 * <pre>
 * if (this.pickupDelay > 0 && this.pickupDelay != 32767) {
 *     this.pickupDelay--;
 * }
 * </pre>
 * We subtract extra ticks from {@code pickupDelay} so items become pickable
 * in the same real time regardless of TPS.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    private int pickupDelay;

    @Unique
    private static final int INFINITE_PICKUP_DELAY = 32767;

    /**
     * After vanilla decrements pickupDelay by 1, we decrement it further.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensatePickupDelay(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!TickAccelerateConfig.INSTANCE.enableItemPickupDelay.get()) return;
        if (this.pickupDelay <= 0 || this.pickupDelay == INFINITE_PICKUP_DELAY) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        if (extra > 0) {
            this.pickupDelay = Math.max(0, this.pickupDelay - extra);
        }
    }
}


