package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Weather cycle TPS compensation.
 *
 * <h3>Vanilla logic (ServerLevel.advanceWeatherCycle)</h3>
 * <pre>
 * clearWeatherTime--; thunderTime--; rainTime--;
 * </pre>
 * <p>These counters decrement once per tick. At low TPS, weather
 * transitions (rain start/stop, thunder start/stop) take longer
 * in real time. We decrement extra ticks so the weather cycle
 * progresses at the correct real-time rate.</p>
 */
@Mixin(ServerLevel.class)
public abstract class WeatherMixin {

    @Shadow
    private ServerLevelData serverLevelData;

    @Unique
    private final float[] tickaccelerate$weatherAccum = new float[1];

    @Inject(method = "advanceWeatherCycle", at = @At("TAIL"))
    private void tickaccelerate$compensateWeatherCycle(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        MinecraftServer server = self.getServer();

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableWeatherCycle) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$weatherAccum);
        if (extra <= 0) return;

        int clearTime = this.serverLevelData.getClearWeatherTime();
        if (clearTime > 0) {
            this.serverLevelData.setClearWeatherTime(Math.max(0, clearTime - extra));
        }

        int thunderTime = this.serverLevelData.getThunderTime();
        if (thunderTime > 0) {
            this.serverLevelData.setThunderTime(Math.max(0, thunderTime - extra));
        }

        int rainTime = this.serverLevelData.getRainTime();
        if (rainTime > 0) {
            this.serverLevelData.setRainTime(Math.max(0, rainTime - extra));
        }
    }
}

