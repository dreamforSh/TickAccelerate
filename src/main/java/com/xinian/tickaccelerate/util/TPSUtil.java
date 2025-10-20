package com.xinian.tickaccelerate.util;

import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class
TPSUtil {
    private static final DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final DecimalFormat dfMissedTicks = new DecimalFormat("0.0000", DecimalFormatSymbols.getInstance(Locale.ROOT));

    // New constants for TPS thresholds
    public static final double TPS_GOOD_THRESHOLD = 15.0;
    public static final double TPS_WARNING_THRESHOLD = 10.0;

    public static String colorizeTPS(double tps, boolean format) {
        if (tps > TPS_GOOD_THRESHOLD) { // Use constant
            return "§a" + (format ? formatTPS(tps) : tps);
        } else if (tps > TPS_WARNING_THRESHOLD) { // Use constant
            return "§e" + (format ? formatTPS(tps) : tps);
        } else {
            return "§c" + (format ? formatTPS(tps) : tps);
        }
    }

    public static String formatTPS(double tps) {
        return df.format(tps);
    }

    public static String formatMissedTicks(double missedTicks) {
        return dfMissedTicks.format(missedTicks);
    }

    public static float tt20(float ticks, boolean limitZero, double tpsMultiplier, @Nullable ResourceLocation resourceLocation) {
        float newTicks = (float) rawTT20(ticks, tpsMultiplier, resourceLocation);

        Integer customTickCap = TickAccelerate.config.getCustomTickCap(resourceLocation);
        if (customTickCap != null && customTickCap > 0) {
            newTicks = Math.min(newTicks, customTickCap);
        }

        if (limitZero) return newTicks > 0 ? newTicks : 1;
        else return newTicks;
    }

    public static int tt20(int ticks, boolean limitZero, double tpsMultiplier, @Nullable ResourceLocation resourceLocation) {
        int newTicks = (int) Math.ceil(rawTT20(ticks, tpsMultiplier, resourceLocation));

        Integer customTickCap = TickAccelerate.config.getCustomTickCap(resourceLocation);
        if (customTickCap != null && customTickCap > 0) {
            newTicks = Math.min(newTicks, customTickCap);
        }

        if (limitZero) return newTicks > 0 ? newTicks : 1;
        else return newTicks;
    }

    public static double tt20(double ticks, boolean limitZero, double tpsMultiplier, @Nullable ResourceLocation resourceLocation) {
        double newTicks = (double) rawTT20(ticks, tpsMultiplier, resourceLocation);

        Integer customTickCap = TickAccelerate.config.getCustomTickCap(resourceLocation);
        if (customTickCap != null && customTickCap > 0) {
            newTicks = Math.min(newTicks, customTickCap);
        }

        if (limitZero) return newTicks > 0 ? newTicks : 1;
        else return newTicks;
    }

    public static double rawTT20(double ticks, double tpsMultiplier, @Nullable ResourceLocation resourceLocation) {
        Double customMultiplier = TickAccelerate.config.getCustomTpsMultiplier(resourceLocation);
        if (customMultiplier != null) {
            tpsMultiplier = customMultiplier;
        }

        if (ticks == 0 || tpsMultiplier == 0) {
            return 0;
        }
        return ticks / tpsMultiplier;
    }
}