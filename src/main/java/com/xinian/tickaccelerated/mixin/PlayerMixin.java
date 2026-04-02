package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attack cooldown TPS compensation.
 * <p>Each tick increments {@code attackStrengthTicker} by {@code 20 / tps} instead of 1.</p>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Shadow
    protected int attackStrengthTicker;

    /**
     * After vanilla increments {@code attackStrengthTicker} by 1, apply extra increments.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateAttackCooldown(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer)) return;
        if (!TickAccelerateConfig.INSTANCE.enableAttackCooldown.get()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        if (multiplier <= 1.0F) return;

        int extraTicks = (int) (multiplier - 1.0F);
        float fraction = multiplier - 1.0F - extraTicks;
        if (fraction > 0 && self.getRandom().nextFloat() < fraction) {
            extraTicks++;
        }
        if (extraTicks > 0) {
            this.attackStrengthTicker += extraTicks;
        }
    }
}
