package com.eternalsnow.state;

import net.minecraft.core.BlockPos;

public final class HeatScanClientData {
    public static volatile BlockPos origin = BlockPos.ZERO;
    public static volatile short[] packedOffsets = new short[0];
    public static volatile short[] deciTemps = new short[0];
    public static volatile long lastUpdateMillis = 0L;

    private HeatScanClientData() {
    }

    public static void update(BlockPos newOrigin, short[] offsets, short[] temps) {
        origin = newOrigin;
        packedOffsets = offsets;
        deciTemps = temps;
        lastUpdateMillis = System.currentTimeMillis();
    }

    public static void clear() {
        packedOffsets = new short[0];
        deciTemps = new short[0];
    }
}
