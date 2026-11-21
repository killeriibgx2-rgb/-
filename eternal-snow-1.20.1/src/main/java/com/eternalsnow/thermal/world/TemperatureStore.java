package com.eternalsnow.thermal.world;

import com.eternalsnow.thermal.engine.Temperature;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Sparse per-block temperature saved with the level.
 *
 * <p>Only temperatures that differ meaningfully from the current outside field
 * are stored. Empty wilderness therefore costs almost nothing, while warm
 * rooms and their walls retain their state exactly when no player is nearby.
 * There is intentionally no offline cooling or catch-up.
 */
public final class TemperatureStore extends SavedData {
    private static final String DATA_NAME = "eternalsnow_temperatures";
    private static final String POSITIONS_KEY = "positions";
    private static final String TEMPERATURE_BITS_KEY = "kelvinBits";

    /** Numerical dust below 0.1 mK is not useful gameplay state. */
    private static final double SPARSE_EPSILON_K = 1.0e-4;

    private final Map<Long, Double> temperatures = new HashMap<>();

    public static TemperatureStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                TemperatureStore::load, TemperatureStore::new, DATA_NAME);
    }

    public static TemperatureStore load(CompoundTag tag) {
        TemperatureStore store = new TemperatureStore();
        long[] keys = tag.getLongArray(POSITIONS_KEY);
        long[] bits = tag.getLongArray(TEMPERATURE_BITS_KEY);
        int n = Math.min(keys.length, bits.length);
        for (int i = 0; i < n; i++) {
            double kelvin = Double.longBitsToDouble(bits[i]);
            if (Double.isFinite(kelvin) && kelvin > 0) {
                store.temperatures.put(keys[i], kelvin);
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] keys = new long[temperatures.size()];
        long[] bits = new long[temperatures.size()];
        int i = 0;
        for (Map.Entry<Long, Double> entry : temperatures.entrySet()) {
            keys[i] = entry.getKey();
            bits[i] = Double.doubleToRawLongBits(entry.getValue());
            i++;
        }
        tag.putLongArray(POSITIONS_KEY, keys);
        tag.putLongArray(TEMPERATURE_BITS_KEY, bits);
        return tag;
    }

    /** Stored temperature, or the ambient value when this block was never touched. */
    public double temperatureAt(BlockPos pos, double fallbackKelvin) {
        Double stored = temperatures.get(pos.asLong());
        return stored == null ? fallbackKelvin : stored;
    }

    /**
     * Store a cell temperature, dropping it again when it has returned to the
     * outside field. The epsilon is far below any visible or consequential
     * temperature difference, and prevents an unbounded map of untouched cold
     * terrain around every player.
     */
    public void setTemperature(BlockPos pos, double kelvin, double fallbackKelvin) {
        Temperature.requireValid(kelvin);
        Temperature.requireValid(fallbackKelvin);
        long key = pos.asLong();
        Double previous = temperatures.get(key);
        if (Math.abs(kelvin - fallbackKelvin) <= SPARSE_EPSILON_K) {
            if (temperatures.remove(key) != null) setDirty();
            return;
        }
        if (previous == null || Double.doubleToLongBits(previous) != Double.doubleToLongBits(kelvin)) {
            temperatures.put(key, kelvin);
            setDirty();
        }
    }

    public int size() {
        return temperatures.size();
    }
}
