package com.xinian.tickaccelerated.mixin.player;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player tick-based TPS compensation.
 *
 * <h3>XP pickup delay</h3>
 * <p>Vanilla: {@code this.takeXpDelay--} per tick. We subtract extra to keep the delay
 * constant in real time.</p>
 *
 * <h3>Sleep timer</h3>
 * <p>Vanilla: {@code this.sleepCounter++} per tick while sleeping, up to 100.
 * We increment extra ticks so the player falls asleep in the same real time.</p>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Shadow
    public int takeXpDelay;

    @Shadow
    private int sleepCounter;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensatePlayerTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer)) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        if (multiplier <= 1.0F) return;

        // XP pickup delay compensation
        if (TickAccelerateConfig.INSTANCE.enableXpPickupDelay.get() && this.takeXpDelay > 0) {
            int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
            if (extra > 0) {
                this.takeXpDelay = Math.max(0, this.takeXpDelay - extra);
            }
        }

        // Sleep timer compensation (isSleeping() is on LivingEntity, accessed via self)
        if (TickAccelerateConfig.INSTANCE.enableSleepTimer.get() && self.isSleeping() && this.sleepCounter > 0) {
            int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
            if (extra > 0) {
                this.sleepCounter = Math.min(100, this.sleepCounter + extra);
            }
        }
    }
}
