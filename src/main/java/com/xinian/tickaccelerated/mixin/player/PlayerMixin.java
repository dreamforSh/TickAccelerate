package com.xinian.tickaccelerated.mixin.player;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player-specific TPS compensation for tick counters in {@link Player#tick()}.
 *
 * <h3>XP pickup delay</h3>
 * <p>Vanilla: {@code this.takeXpDelay--} per tick. We subtract extra to keep
 * the delay constant in real time.</p>
 *
 * <h3>Sleep timer</h3>
 * <p>Vanilla: {@code this.sleepCounter++} per tick while sleeping, up to 100.
 * We increment extra so the player falls asleep in the same real time.</p>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Shadow public int takeXpDelay;
    @Shadow private int sleepCounter;

    @Unique private final float[] tickaccelerate$xpAccum = new float[1];
    @Unique private final float[] tickaccelerate$sleepAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensatePlayerTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer)) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        float multiplier = TpsHelper.getSpeedMultiplier(server);
        if (multiplier <= 1.0F) return;

        if (config.enableXpPickupDelay && this.takeXpDelay > 0) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$xpAccum);
            if (extra > 0) {
                this.takeXpDelay = Math.max(0, this.takeXpDelay - extra);
            }
        }

        if (config.enableSleepTimer
                && this.sleepCounter > 0 && this.sleepCounter < 100) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$sleepAccum);
            if (extra > 0) {
                this.sleepCounter = Math.min(100, this.sleepCounter + extra);
            }
        }
    }
}
