package com.xinian.tickaccelerated.util;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/**
 * TPS compensation utility with per-tick caching.
 * <p>Core formula: {@code newTicks = originalTicks * tps / 20}</p>
 * <p>{@code tps = min(20, 1000 / mspt)}, clamped to [{@code minTps}, 20].</p>
 */
public final class TpsHelper {

    public static final float MAX_TPS = 20.0F;

    private static int cachedTickCount = -1;
    private static float cachedTps = MAX_TPS;
    private static float cachedFactor = 1.0F;
    private static float cachedMultiplier = 1.0F;

    private TpsHelper() {}

    /**
     * Returns the current server TPS, clamped to [minTps, 20].
     * Values are cached per server tick for efficiency.
     */
    public static float getTps(MinecraftServer server) {
        if (server == null) return MAX_TPS;
        int tick = server.getTickCount();
        if (tick != cachedTickCount) {
            cachedTickCount = tick;
            float mspt = server.getCurrentSmoothedTickTime();
            if (mspt <= 0.0F) {
                cachedTps = MAX_TPS;
            } else {
                cachedTps = Math.max(getMinTps(), Math.min(MAX_TPS, 1000.0F / mspt));
            }
            cachedFactor = cachedTps / MAX_TPS;
            cachedMultiplier = MAX_TPS / cachedTps;
        }
        return cachedTps;
    }

    /**
     * Returns the current server TPS from an entity context.
     * Returns 20 on the client side.
     */
    public static float getTps(Entity entity) {
        if (entity == null || entity.level().isClientSide()) return MAX_TPS;
        return getTps(entity.level().getServer());
    }

    /**
     * Tick count scaling factor: {@code tps / 20}.
     * <p>Range: [minTps/20, 1.0]. Multiply original tick counts by this
     * to shorten durations proportionally.</p>
     */
    public static float getTickFactor(MinecraftServer server) {
        getTps(server);
        return cachedFactor;
    }

    /** @see #getTickFactor(MinecraftServer) */
    public static float getTickFactor(Entity entity) {
        getTps(entity);
        return cachedFactor;
    }

    /**
     * Per-tick speed multiplier: {@code 20 / tps}.
     * <p>Range: [1.0, 20/minTps]. Multiply per-tick increments by this
     * to make them progress at correct real-time speed.</p>
     */
    public static float getSpeedMultiplier(MinecraftServer server) {
        getTps(server);
        return cachedMultiplier;
    }

    /** @see #getSpeedMultiplier(MinecraftServer) */
    public static float getSpeedMultiplier(Entity entity) {
        getTps(entity);
        return cachedMultiplier;
    }

    /**
     * Computes extra ticks using stochastic rounding.
     * Use when per-instance accumulator state is not available.
     *
     * @param multiplier speed multiplier ({@code >= 1.0})
     * @param random     a random float in [0, 1)
     * @return extra ticks to add (0 when multiplier &le; 1)
     */
    public static int computeExtraTicks(float multiplier, float random) {
        if (multiplier <= 1.0F) return 0;
        float extra = multiplier - 1.0F;
        int whole = (int) extra;
        float fraction = extra - whole;
        if (fraction > 0 && random < fraction) {
            whole++;
        }
        return whole;
    }

    /**
     * Deterministic extra-tick calculation with fractional accumulator.
     * Avoids random jitter by accumulating fractional ticks between calls.
     *
     * @param multiplier  speed multiplier ({@code >= 1.0})
     * @param accumulator single-element array holding the carried fraction
     * @return extra whole ticks to add this tick
     */
    public static int computeExtraTicksDeterministic(float multiplier, float[] accumulator) {
        if (multiplier <= 1.0F) {
            accumulator[0] = 0.0F;
            return 0;
        }
        float total = (multiplier - 1.0F) + accumulator[0];
        int whole = (int) total;
        accumulator[0] = total - whole;
        return whole;
    }

    /**
     * Scales a tick duration by the tick factor.
     * {@code result = max(1, round(ticks * tps / 20))}
     */
    public static int scaleDuration(int ticks, MinecraftServer server) {
        float factor = getTickFactor(server);
        if (factor >= 1.0F) return ticks;
        return Math.max(1, Math.round(ticks * factor));
    }

    private static float getMinTps() {
        try {
            return TickAccelerateConfig.INSTANCE.minTps.get().floatValue();
        } catch (Exception e) {
            return 5.0F;
        }
    }
}
