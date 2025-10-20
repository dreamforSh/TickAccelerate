package com.xinian.tickaccelerate.mixin.item;

import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow private int pickupDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void pickupDelayTT20(CallbackInfo ci) {
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.pickupAcceleration()) return;
        if (((Entity) (Object) this).level().isClientSide()) return;

        if (this.pickupDelay == 0) return;

        int ticksToApply = TickAccelerate.TPS_CALCULATOR.applicableMissedTicks();
        int cap = TickAccelerate.TPS_CALCULATOR.getEffectiveTickCap();
        if (cap > 0) {
            ticksToApply = Math.min(ticksToApply, cap);
        }

        if (this.pickupDelay - ticksToApply <= 0) {
            this.pickupDelay = 0;
            return;
        }

        this.pickupDelay = this.pickupDelay - ticksToApply;
    }
}