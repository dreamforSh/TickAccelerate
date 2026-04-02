package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dropped item pickup delay TPS compensation.
 * <p>Each tick decreases {@code pickupDelay} by {@code 20 / tps} instead of 1.</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    private int pickupDelay;

    /**
     * After vanilla decrements {@code pickupDelay} by 1, apply extra reduction.
     * Sentinel value 32767 (never pickup) is left untouched.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensatePickupDelay(CallbackInfo ci) {
        if (!TickAccelerateConfig.INSTANCE.enableItemPickupDelay.get()) return;
        if (this.pickupDelay <= 0 || this.pickupDelay == 32767) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        if (multiplier <= 1.0F) return;

        int extraTicks = (int) (multiplier - 1.0F);
        float fraction = multiplier - 1.0F - extraTicks;
        if (fraction > 0 && server.overworld().getRandom().nextFloat() < fraction) {
            extraTicks++;
        }
        if (extraTicks > 0) {
            this.pickupDelay = Math.max(0, this.pickupDelay - extraTicks);
        }
    }
}

