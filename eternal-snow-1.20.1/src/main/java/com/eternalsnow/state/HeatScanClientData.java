package com.eternalsnow.state;

import net.minecraft.core.BlockPos;

public final class HeatScanClientData {
    /** Coarse 12x8 screen bins for the cheap full-screen thermal pass (built once per packet). */
    public static final int THERMAL_COLS = 12;
    public static final int THERMAL_ROWS = 8;
    public static volatile int[] thermalBins = new int[THERMAL_COLS * THERMAL_ROWS];
    public static volatile BlockPos origin = BlockPos.ZERO;
    public static volatile short[] packedOffsets = new short[0];
    public static volatile short[] deciTemps = new short[0];
    public static volatile long lastUpdateMillis = 0L;

    // v2.6.0: relative palette + two world-anchored markers (hottest / coldest)
    public static volatile int mode = 0;
    public static volatile float minC = -25.0F;
    public static volatile float maxC = 55.0F;
    public static volatile long hotPos = Long.MIN_VALUE;
    public static volatile float hotC = 0.0F;
    public static volatile long coldPos = Long.MIN_VALUE;
    public static volatile float coldC = 0.0F;

    // Screen projection of the markers, refreshed once per rendered frame by HeatVisionRenderer.
    public static volatile float hotScreenX = -1.0F, hotScreenY = -1.0F;
    public static volatile float coldScreenX = -1.0F, coldScreenY = -1.0F;
    public static volatile boolean hotVisible = false, coldVisible = false;

    private HeatScanClientData() {
    }

    public static void update(BlockPos newOrigin, short[] offsets, short[] temps,
                              int newMode, float newMinC, float newMaxC,
                              long newHotPos, float newHotC, long newColdPos, float newColdC) {
        origin = newOrigin;
        packedOffsets = offsets;
        deciTemps = temps;
        mode = newMode;
        minC = newMinC;
        maxC = newMaxC;
        hotPos = newHotPos;
        hotC = newHotC;
        coldPos = newColdPos;
        coldC = newColdC;
        buildThermalBins(offsets, temps, newMinC, newMaxC);
        lastUpdateMillis = System.currentTimeMillis();
    }

    private static void buildThermalBins(short[] offsets, short[] temps, float lo, float hi) {
        int[] sum = new int[THERMAL_COLS * THERMAL_ROWS];
        int[] count = new int[THERMAL_COLS * THERMAL_ROWS];
        float span = Math.max(0.6F, hi - lo);
        for (int i = 0; i < offsets.length && i < temps.length; i++) {
            int packed = offsets[i] & 0xFFFF;
            int dx = (packed & 0x1F) - 10;       // ClientboundHeatGridPacket.RADIUS
            int dy = ((packed >> 5) & 0x1F) - 10;
            int col = Math.max(0, Math.min(THERMAL_COLS - 1, (dx + 10) * THERMAL_COLS / 20));
            int row = Math.max(0, Math.min(THERMAL_ROWS - 1, (10 - dy) * THERMAL_ROWS / 20));
            int idx = row * THERMAL_COLS + col;
            sum[idx] += temps[i];
            count[idx]++;
        }
        int[] bins = new int[THERMAL_COLS * THERMAL_ROWS];
        for (int i = 0; i < bins.length; i++) {
            if (count[i] == 0) {
                continue;
            }
            float t = ((sum[i] / (float) count[i]) * 0.1F - lo) / span;
            t = Math.max(0.0F, Math.min(1.0F, t));
            // blue -> cyan -> yellow -> red, alpha kept low so the world stays visible
            int r, g, b;
            if (t < 0.34F) {
                float q = t / 0.34F;
                r = Math.round(35 + q * 5);
                g = Math.round(70 + q * 150);
                b = 235;
            } else if (t < 0.67F) {
                float q = (t - 0.34F) / 0.33F;
                r = Math.round(40 + q * 215);
                g = Math.round(220 - q * 5);
                b = Math.round(235 - q * 175);
            } else {
                float q = (t - 0.67F) / 0.33F;
                r = 255;
                g = Math.round(215 - q * 175);
                b = Math.round(60 - q * 20);
            }
            bins[i] = 0x3A000000 | (r << 16) | (g << 8) | b;
        }
        thermalBins = bins;
    }

    public static void setMarkerScreen(float hx, float hy, boolean hv, float cx, float cy, boolean cv) {
        hotScreenX = hx;
        hotScreenY = hy;
        hotVisible = hv;
        coldScreenX = cx;
        coldScreenY = cy;
        coldVisible = cv;
    }

    public static boolean markersFresh() {
        return System.currentTimeMillis() - lastUpdateMillis < 4000L;
    }

    public static void clear() {
        packedOffsets = new short[0];
        deciTemps = new short[0];
        hotPos = Long.MIN_VALUE;
        coldPos = Long.MIN_VALUE;
        hotVisible = false;
        coldVisible = false;
        mode = 0;
        thermalBins = new int[THERMAL_COLS * THERMAL_ROWS];
        lastUpdateMillis = 0L;
    }
}
