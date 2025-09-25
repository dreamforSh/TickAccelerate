package com.xinian.tickaccelerate.util;

import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TPSCalculator {

    public Long lastTick;
    public Long currentTick;

    private double trueMissedTicks = 0;
    private double compensationTicks = 0;
    private final List<Double> tpsHistory = new CopyOnWriteArrayList<>();
    private static final int historyLimit = 40;

    public static final int MAX_TPS = 20;
    public static final int FULL_TICK = 50;

    private static final double SMOOTHING_FACTOR = 0.1;
    private double smoothedMSPT = 50.0;

    public static float tpsMultiplier = 1.0f;

    public TPSCalculator() {
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
    }

    private void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (currentTick != null) {
            lastTick = currentTick;
        }

        currentTick = System.currentTimeMillis();

        // Update smoothed MSPT using EMA
        long currentMSPT = getMSPT();
        smoothedMSPT = (currentMSPT * SMOOTHING_FACTOR) + (smoothedMSPT * (1 - SMOOTHING_FACTOR));

        addToHistory(getTPS());
        clearMissedTicks();
        missedTick();

        double mostAccurateTps = getMostAccurateTPS();
        if (mostAccurateTps > 0) {
            tpsMultiplier = (float) (MAX_TPS / mostAccurateTps);
        } else {
            tpsMultiplier = 1.0f;
        }
    }

    public int getEffectiveTickCap() {
        return getEffectiveTickCap(null);
    }

    public int getEffectiveTickCap(@Nullable ResourceLocation blockId) {
        int baseCap;
        if (blockId == null) {
            baseCap = TickAccelerate.config.getTickRepeatCap();
        } else {
            baseCap = TickAccelerate.config.getCapForBlock(blockId);
        }

        if (baseCap < 0) { // Handle "no limit"
            return -1;
        }

        if (!TickAccelerate.config.dynamicCapEnabled()) {
            return baseCap;
        }

        long currentMspt = getMSPT();
        int threshold = TickAccelerate.config.getDynamicCapMsptThreshold();

        if (threshold > 0 && currentMspt > threshold) {
            return 0;
        } else {
            return baseCap;
        }
    }

    private void addToHistory(double tps) {
        if (tpsHistory.size() >= historyLimit) {
            tpsHistory.remove(0);
        }

        tpsHistory.add(tps);
    }

    public long getMSPT() {
        if (lastTick == null || currentTick == null) return 0;
        return currentTick - lastTick;
    }

    public double getSmoothedMSPT() {
        return smoothedMSPT;
    }

    public double getAverageTPS() {
        return tpsHistory.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(MAX_TPS);
    }

    public double getTPS() {
        if (smoothedMSPT <= 0) return MAX_TPS;
        double tps = 1000 / smoothedMSPT;
        return Math.min(tps, MAX_TPS);
    }

    public void missedTick() {
        double currentSmoothedMSPT = getSmoothedMSPT();
        if (currentSmoothedMSPT <= 0) return;

        // Calculate true missed ticks
        double missed = (currentSmoothedMSPT / (double) FULL_TICK) - 1;
        this.trueMissedTicks += Math.max(0, missed);

        // Handle extra compensation
        if (TickAccelerate.config.extraCompensationEnabled() && currentSmoothedMSPT > TickAccelerate.config.getExtraCompensationMsptThreshold()) {
            double compensationAmount = (double) TickAccelerate.config.getExtraCompensationAmountMs() / FULL_TICK;
            this.compensationTicks += compensationAmount;
        }
    }

    public double getMostAccurateTPS() {
        return Math.min(getTPS(), getAverageTPS());
    }

    public double getAllMissedTicks() {
        return this.trueMissedTicks;
    }

    public int applicableMissedTicks() {
        return (int) Math.floor(this.trueMissedTicks + this.compensationTicks);
    }

    public void clearMissedTicks() {
        int ticksUsed = applicableMissedTicks();
        if (ticksUsed == 0) return;

        double totalTicks = this.trueMissedTicks + this.compensationTicks;
        if (totalTicks <= 0) return;

        double trueProportion = this.trueMissedTicks / totalTicks;

        this.trueMissedTicks -= ticksUsed * trueProportion;
        this.compensationTicks -= ticksUsed * (1 - trueProportion);
    }

    public void resetMissedTicks() {
        this.trueMissedTicks = 0;
        this.compensationTicks = 0;
    }
}
