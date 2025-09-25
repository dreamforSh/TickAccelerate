package com.xinian.tickaccelerate.util;

import com.xinian.tickaccelerate.TickAccelerate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class
TPSUtil {
    private static final DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final DecimalFormat dfMissedTicks = new DecimalFormat("0.0000", DecimalFormatSymbols.getInstance(Locale.ROOT));

    public static String colorizeTPS(double tps, boolean format) {
        if (tps > 15) {
            return "§a" + (format ? formatTPS(tps) : tps);
        } else if (tps > 10) {
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

    public static float tt20(float ticks, boolean limitZero) {
        float newTicks = (float) rawTT20(ticks);

        if (limitZero) return newTicks > 0 ? newTicks : 1;
        else return newTicks;
    }

    public static int tt20(int ticks, boolean limitZero) {
        int newTicks = (int) Math.ceil(rawTT20(ticks));

        if (limitZero) return newTicks > 0 ? newTicks : 1;
        else return newTicks;
    }

    public static double tt20(double ticks, boolean limitZero) {
        double newTicks = (double) rawTT20(ticks);

        if (limitZero) return newTicks > 0 ? newTicks : 1;
        else return newTicks;
    }

    public static double rawTT20(double ticks) {
        if (ticks == 0 || TPSCalculator.tpsMultiplier == 0) {
            return 0;
        }
        return ticks / TPSCalculator.tpsMultiplier;
    }
}