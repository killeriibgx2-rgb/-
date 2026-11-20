package com.eternalsnow.thermal.world;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.thermal.engine.Constants;
import net.minecraft.server.level.ServerLevel;

/**
 * Outside temperature of a level.
 *
 * <p>Currently one configured value for the whole world. The interface exists
 * so biome, altitude and time of day can be layered in without touching the
 * engine: the engine already accepts ambient as a function of position.
 */
public final class AmbientField {
    /** Ambient temperature for a level, K. */
    public double kelvinAt(ServerLevel level) {
        // In the Nether and the End vanilla already defines a temperature; using
        // it keeps the mod from fighting dimensions it was not designed for.
        if (level.dimension() == net.minecraft.world.level.Level.NETHER) {
            return Constants.toKelvin(40);
        }
        if (level.dimension() == net.minecraft.world.level.Level.END) {
            return Constants.toKelvin(-20);
        }
        return Constants.toKelvin(ThermalConfig.AMBIENT_CELSIUS.get());
    }

    /** Ambient temperature in Celsius, for commands and the panel. */
    public double celsiusAt(ServerLevel level) {
        return Constants.toCelsius(kelvinAt(level));
    }
}
