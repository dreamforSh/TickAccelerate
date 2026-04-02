package com.xinian.tickaccelerated.util;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/**
 * TPS compensation utility.
 * <p>Core formula: {@code newTicks = originalTicks * tps / 20}</p>
 * <p>{@code tps = min(20, 1000 / mspt)}, clamped to [{@code minTps}, 20].</p>
 */
public final class TpsHelper {

    public static final float MAX_TPS = 20.0F;

    private TpsHelper() {}

    /* ──────────────────── TPS query ──────────────────── */

    /**
     * Returns the current server TPS, clamped to [minTps, 20].
     */
    public static float getTps(MinecraftServer server) {
        if (server == null) return MAX_TPS;
        float mspt = server.getCurrentSmoothedTickTime();
        if (mspt <= 0.0F) return MAX_TPS;
        float tps = Math.min(MAX_TPS, 1000.0F / mspt);
        return Math.max(getMinTps(), tps);
    }

    /**
     * Returns the current server TPS from an entity context.
     * Returns 20 on the client side.
     */
    public static float getTps(Entity entity) {
        if (entity == null || entity.level().isClientSide()) return MAX_TPS;
        return getTps(entity.level().getServer());
    }

    /* ──────────── Tick‑count scaling (duration → fewer ticks) ──────────── */

    /**
     * Tick count scaling factor: {@code tps / 20}.
     * <p>Range: [minTps/20, 1.0]. Multiply original tick counts by this.</p>
     * <ul>
     *   <li>20 TPS → 1.0 (unchanged)</li>
     *   <li>10 TPS → 0.5 (halved)</li>
     *   <li>5  TPS → 0.25</li>
     * </ul>
     */
    public static float getTickFactor(MinecraftServer server) {
        return getTps(server) / MAX_TPS;
    }

    /** @see #getTickFactor(MinecraftServer) */
    public static float getTickFactor(Entity entity) {
        return getTps(entity) / MAX_TPS;
    }

    /* ──────────── Per‑tick speed multiplier (increment → faster) ──────────── */

    /**
     * Per-tick speed multiplier: {@code 20 / tps}.
     * <p>Range: [1.0, 20/minTps]. Multiply per-tick increments by this.</p>
     * <ul>
     *   <li>20 TPS → 1.0 (unchanged)</li>
     *   <li>10 TPS → 2.0 (each tick counts double)</li>
     *   <li>5  TPS → 4.0</li>
     * </ul>
     */
    public static float getSpeedMultiplier(MinecraftServer server) {
        return MAX_TPS / getTps(server);
    }

    /** @see #getSpeedMultiplier(MinecraftServer) */
    public static float getSpeedMultiplier(Entity entity) {
        return MAX_TPS / getTps(entity);
    }

    /* ──────────── Stochastic extra‑tick helper ──────────── */

    /**
     * Computes how many <em>extra</em> ticks to apply on top of vanilla's single decrement/increment.
     * Uses stochastic rounding for the fractional part.
     *
     * @param multiplier speed multiplier ({@code >= 1.0})
     * @param random     a random float in [0, 1)
     * @return extra ticks to add (0 when multiplier &lt;= 1)
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

    /* ──────────── Config accessor ──────────── */

    private static float getMinTps() {
        try {
            return TickAccelerateConfig.INSTANCE.minTps.get().floatValue();
        } catch (Exception e) {
            return 5.0F;
        }
    }
}

