package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Natural regeneration / starvation timer TPS compensation.
 * <p>Each tick increments {@code tickTimer} by {@code 20 / tps} instead of 1.</p>
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int tickTimer;

    /**
     * After vanilla increments {@code tickTimer}, apply extra increments.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateFoodTimer(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        if (!TickAccelerateConfig.INSTANCE.enableFoodRegen.get()) return;
        if (this.tickTimer <= 0) return;

        float multiplier = TpsHelper.getSpeedMultiplier(player);
        if (multiplier <= 1.0F) return;

        int extraTicks = (int) (multiplier - 1.0F);
        float fraction = multiplier - 1.0F - extraTicks;
        if (fraction > 0 && player.getRandom().nextFloat() < fraction) {
            extraTicks++;
        }
        if (extraTicks > 0) {
            this.tickTimer += extraTicks;
        }
    }
}
