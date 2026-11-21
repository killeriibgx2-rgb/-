package com.eternalsnow.thermal.world;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.thermal.engine.Constants;
import net.minecraft.server.level.ServerLevel;

/** Outside field adapter. The current value is an explicit global fallback, not a climate model. */
public final class AmbientField {
    /** Global fallback temperature in Kelvin. */
    public double kelvinAt(ServerLevel level) {
        return Constants.toKelvin(ThermalConfig.AMBIENT_CELSIUS.get());
    }

    /** Same field in Celsius, used by the body model and debug panel. */
    public double celsiusAt(ServerLevel level) {
        return Constants.toCelsius(kelvinAt(level));
    }
}
