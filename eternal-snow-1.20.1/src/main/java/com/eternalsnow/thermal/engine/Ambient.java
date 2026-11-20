package com.eternalsnow.thermal.engine;

/**
 * The world outside the simulated volume.
 *
 * <p>Deliberately a function of position rather than a constant, because the
 * real thing varies with biome, altitude, time of day and weather. Stage 2 only
 * ships {@link #constant}, but the shape is the contract the adapter will fill.
 */
@FunctionalInterface
public interface Ambient {
    /** Outside air temperature at a world position, K. */
    double temperatureKelvin(double x, double y, double z);

    /** A uniform outside temperature. */
    static Ambient constant(double kelvin) {
        double t = Temperature.requireValid(kelvin);
        return (x, y, z) -> t;
    }

    /** Air density from the ideal gas law at standard pressure, kg/m³. */
    static double airDensity(double temperatureKelvin) {
        return Constants.STANDARD_PRESSURE / (Constants.SPECIFIC_GAS_AIR * temperatureKelvin);
    }
}
