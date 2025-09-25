package com.xinian.tickaccelerate.mixin;

import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected abstract void tickEffects();

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;tickEffects()V"))
    private void fixPotionDelayTick(CallbackInfo ci) {
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.potionEffectAcceleration()) return;
        if (((Entity) (Object) this).getLevel().isClientSide()) return;

        int ticksToApply = TickAccelerate.TPS_CALCULATOR.applicableMissedTicks();
        int cap = TickAccelerate.TPS_CALCULATOR.getEffectiveTickCap();
        if (cap > 0) {
            ticksToApply = Math.min(ticksToApply, cap);
        }

        for (int i = 0; i < ticksToApply; i++) {
            tickEffects();
        }
    }
}