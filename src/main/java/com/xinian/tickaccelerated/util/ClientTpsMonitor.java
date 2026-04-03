package com.xinian.tickaccelerated.util;

/**
 * Tracks effective client-side TPS by measuring the time between entity ticks.
 * <p>On the client, entity ticks are driven by network sync with the server.
 * When the server runs at low TPS, client ticks also slow down, causing
 * tick-based animations to appear sluggish.</p>
 *
 * <p>This monitor provides the same multiplier/factor API as {@link TpsHelper},
 * but derived from client-side measurements rather than server MSPT.</p>
 */
public final class ClientTpsMonitor {

    private static final float MAX_TPS = 20.0F;
    private static final float SMOOTHING = 0.08F;
    private static final float DEFAULT_MSPT = 50.0F;
    private static final float MIN_MSPT = 1.0F;
    private static final float MAX_MSPT = 5000.0F;
    private static final float MIN_TPS = 5.0F;

    private static long lastTickNanos = 0;
    private static float smoothedMspt = DEFAULT_MSPT;

    private ClientTpsMonitor() {}

    /**
     * Called once per client level tick to record timing.
     */
    public static void onTick() {
        long now = System.nanoTime();
        if (lastTickNanos > 0) {
            float mspt = (now - lastTickNanos) / 1_000_000.0F;
            mspt = Math.max(MIN_MSPT, Math.min(MAX_MSPT, mspt));
            smoothedMspt = smoothedMspt + (mspt - smoothedMspt) * SMOOTHING;
        }
        lastTickNanos = now;
    }

    /** Resets state (e.g. when disconnecting). */
    public static void reset() {
        lastTickNanos = 0;
        smoothedMspt = DEFAULT_MSPT;
    }

    public static float getMspt() {
        return smoothedMspt;
    }

    public static float getTps() {
        float tps = Math.min(MAX_TPS, 1000.0F / smoothedMspt);
        return Math.max(MIN_TPS, tps);
    }

    /** @see TpsHelper#getSpeedMultiplier(net.minecraft.server.MinecraftServer) */
    public static float getSpeedMultiplier() {
        return MAX_TPS / getTps();
    }

    /** @see TpsHelper#getTickFactor(net.minecraft.server.MinecraftServer) */
    public static float getTickFactor() {
        return getTps() / MAX_TPS;
    }

    /**
     * Deterministic extra-tick calculation with fractional accumulator (client-side).
     *
     * @param accumulator single-element array holding the carried fraction
     * @return extra whole ticks to add this tick
     * @see TpsHelper#computeExtraTicksDeterministic(float, float[])
     */
    public static int computeExtraTicksDeterministic(float[] accumulator) {
        float multiplier = getSpeedMultiplier();
        if (multiplier <= 1.0F) {
            accumulator[0] = 0.0F;
            return 0;
        }
        float total = (multiplier - 1.0F) + accumulator[0];
        int whole = (int) total;
        accumulator[0] = total - whole;
        return whole;
    }
}

