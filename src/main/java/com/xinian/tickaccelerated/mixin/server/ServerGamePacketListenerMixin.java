package com.xinian.tickaccelerated.mixin.server;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TPS compensation for tick-based anti-cheat and spam protection in
 * {@link ServerGamePacketListenerImpl}.
 *
 * <h3>Flying kick</h3>
 * <p>Scales the {@code getMaximumFlyingTicks} threshold by the speed
 * multiplier so the effective real-time grace period stays ~4 seconds
 * regardless of TPS.</p>
 *
 * <h3>Spam protection</h3>
 * <p>{@code chatSpamTickCount} and {@code dropSpamTickCount} decrement
 * by 1 per tick. At low TPS they decay slower in real time, making
 * spam protection trigger on normal activity. We decrement extra ticks
 * so the decay rate matches real-time expectations.</p>
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private int chatSpamTickCount;

    @Shadow
    private int dropSpamTickCount;

    @Unique
    private final float[] tickaccelerate$chatSpamAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$dropSpamAccum = new float[1];

    /**
     * Scale the {@code getMaximumFlyingTicks} return value by the speed multiplier
     * so the real-time grace period stays constant regardless of TPS.
     */
    @Inject(method = "getMaximumFlyingTicks", at = @At("RETURN"), cancellable = true)
    private void tickaccelerate$scaleMaxFlyingTicks(Entity entity, CallbackInfoReturnable<Integer> cir) {
        try {
            MinecraftServer server = this.player.getServer();
            if (server == null) return;

            ConfigSnapshot config = ConfigSnapshot.get(server);
            if (!config.enableAntiKick) return;

            float multiplier = TpsHelper.getSpeedMultiplier(server);
            if (multiplier <= 1.0F) return;

            int original = cir.getReturnValue();
            if (original == Integer.MAX_VALUE) return;

            cir.setReturnValue(Math.min(Integer.MAX_VALUE, (int) (original * multiplier)));
        } catch (Exception ignored) {
        }
    }

    /**
     * After vanilla's per-tick spam counter decay, apply extra decrements
     * so the counters decay at a real-time rate.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateSpamCounters(CallbackInfo ci) {
        try {
            MinecraftServer server = this.player.getServer();
            if (server == null) return;

            ConfigSnapshot config = ConfigSnapshot.get(server);
            if (!config.enableAntiKick) return;

            float multiplier = TpsHelper.getSpeedMultiplier(server);
            if (multiplier <= 1.0F) return;

            if (this.chatSpamTickCount > 0) {
                int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$chatSpamAccum);
                if (extra > 0) {
                    this.chatSpamTickCount = Math.max(0, this.chatSpamTickCount - extra);
                }
            }

            if (this.dropSpamTickCount > 0) {
                int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$dropSpamAccum);
                if (extra > 0) {
                    this.dropSpamTickCount = Math.max(0, this.dropSpamTickCount - extra);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
