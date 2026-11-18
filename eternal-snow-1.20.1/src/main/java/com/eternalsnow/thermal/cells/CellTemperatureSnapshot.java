package com.eternalsnow.thermal.cells;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Versioned per-chunk temperature payload, not a global world file. Only absolute
 * temperatures and block positions persist. Materials/topology must be reconstructed
 * from the current chunk on load; no timestamp means no offline catch-up.
 * The world adapter must attach this payload to chunk persistence before activation.
 */
public final class CellTemperatureSnapshot {
    public static final int VERSION = 1;
    private static final int MAX_CELLS = 65536;
    private final long[] positions;
    private final double[] temperatures;

    public CellTemperatureSnapshot(long[] positions, double[] temperatures) {
        if (positions.length != temperatures.length || positions.length > MAX_CELLS)
            throw new IllegalArgumentException("Invalid snapshot size");
        java.util.Set<Long> unique = new java.util.HashSet<>();
        for (int i = 0; i < positions.length; i++) {
            if (!unique.add(positions[i]) || !Double.isFinite(temperatures[i]))
                throw new IllegalArgumentException("Duplicate cell or invalid temperature");
        }
        this.positions = positions.clone();
        this.temperatures = temperatures.clone();
    }

    public long[] positions() { return positions.clone(); }
    public double[] temperatures() { return temperatures.clone(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", VERSION);
        tag.putLongArray("positions", positions);
        // Lossless bit storage, compact primitive arrays rather than one compound per cell.
        long[] bits = new long[temperatures.length];
        for (int i = 0; i < bits.length; i++) bits[i] = Double.doubleToLongBits(temperatures[i]);
        tag.putLongArray("temperatureBits", bits);
        return tag;
    }

    public static CellTemperatureSnapshot load(CompoundTag tag) {
        if (!tag.contains("version", Tag.TAG_INT) || tag.getInt("version") != VERSION)
            throw new IllegalArgumentException("Unsupported thermal snapshot version");
        if (!tag.contains("positions", Tag.TAG_LONG_ARRAY) || !tag.contains("temperatureBits", Tag.TAG_LONG_ARRAY))
            throw new IllegalArgumentException("Incomplete thermal snapshot");
        long[] positions = tag.getLongArray("positions");
        long[] bits = tag.getLongArray("temperatureBits");
        if (bits.length != positions.length || bits.length > MAX_CELLS)
            throw new IllegalArgumentException("Invalid thermal snapshot arrays");
        double[] temperatures = new double[bits.length];
        for (int i = 0; i < bits.length; i++) temperatures[i] = Double.longBitsToDouble(bits[i]);
        return new CellTemperatureSnapshot(positions, temperatures);
    }
}
