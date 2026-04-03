package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mob spawner delay TPS compensation.
 *
 * <h3>Vanilla logic (BaseSpawner.serverTick)</h3>
 * <pre>
 * if (this.spawnDelay &gt; 0) { this.spawnDelay--; }
 * else { // spawn mobs }
 * </pre>
 * <p>At low TPS, the spawner delay ticks down slower in real time,
 * reducing mob spawn rates. We decrement extra ticks so spawners
 * produce mobs at the correct real-time rate.</p>
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {

    @Shadow
    private int spawnDelay;

    @Unique
    private final float[] tickaccelerate$spawnerAccum = new float[1];

    @Inject(method = "serverTick", at = @At("TAIL"))
    private void tickaccelerate$compensateSpawnerDelay(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (this.spawnDelay <= 0) return;

        ConfigSnapshot config = ConfigSnapshot.get(level.getServer());
        if (!config.enableSpawnerCooldown) return;

        float multiplier = TpsHelper.getSpeedMultiplier(level.getServer());
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$spawnerAccum);
        if (extra > 0) {
            this.spawnDelay = Math.max(0, this.spawnDelay - extra);
        }
    }
}
