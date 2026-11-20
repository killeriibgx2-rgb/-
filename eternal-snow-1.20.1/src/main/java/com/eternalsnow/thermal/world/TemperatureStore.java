package com.eternalsnow.thermal.world;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-block temperature, persisted with the level.
 *
 * <p>This is what makes the freeze rule work: the numbers simply stay on disk
 * untouched while nobody is near, and reading them back is the whole of the
 * "catch-up". No timers, no exponentials, no seams.
 *
 * <p>Storage is a plain map keyed by packed position. Only positions the engine
 * has actually touched are written, so an empty world costs nothing.
 */
public final class TemperatureStore extends SavedData {
    private static final String DATA_NAME = "eternalsnow_temperatures";

    private final Map<Long, Float> temperatures = new HashMap<>();

    public static TemperatureStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                TemperatureStore::load, TemperatureStore::new, DATA_NAME);
    }

    public static TemperatureStore load(CompoundTag tag) {
        TemperatureStore store = new TemperatureStore();
        long[] keys = tag.getLongArray("positions");
        // CompoundTag in 1.20.1 has no float array, so temperatures travel as
        // the raw bits of a double in a parallel long array.
        long[] bits = tag.getLongArray("kelvinBits");
        int n = Math.min(keys.length, bits.length);
        for (int i = 0; i < n; i++) {
            store.temperatures.put(keys[i], (float) Double.longBitsToDouble(bits[i]));
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] keys = new long[temperatures.size()];
        long[] bits = new long[temperatures.size()];
        int i = 0;
        for (Map.Entry<Long, Float> entry : temperatures.entrySet()) {
            keys[i] = entry.getKey();
            bits[i] = Double.doubleToRawLongBits(entry.getValue());
            i++;
        }
        tag.putLongArray("positions", keys);
        tag.putLongArray("kelvinBits", bits);
        return tag;
    }

    /** Stored temperature, or the ambient value when this block was never touched. */
    public double temperatureAt(BlockPos pos, double fallbackKelvin) {
        Float stored = temperatures.get(pos.asLong());
        return stored == null ? fallbackKelvin : stored;
    }

    public void setTemperature(BlockPos pos, double kelvin) {
        temperatures.put(pos.asLong(), (float) kelvin);
        setDirty();
    }

    public int size() {
        return temperatures.size();
    }
}
