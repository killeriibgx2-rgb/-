package com.eternalsnow.thermal.engine;

import java.util.Objects;

/**
 * One simulation cell: 1×1×1 m of some material at some temperature.
 *
 * <p>Mutable by design — a grid step touches every cell thousands of times and
 * allocating records per step is how a physics loop becomes the server's
 * bottleneck. Everything that enters or leaves is accounted for by
 * {@link ThermalGrid}, never by touching {@link #temperatureKelvin} directly.
 */
public final class ThermalCell {
    private final Material material;
    private final double heatCapacity;
    private double temperatureKelvin;
    private double externalPowerWatts;

    public ThermalCell(Material material, double temperatureKelvin) {
        this.material = Objects.requireNonNull(material, "material");
        this.heatCapacity = material.cellHeatCapacity();
        this.temperatureKelvin = Temperature.requireValid(temperatureKelvin);
    }

    public Material material() {
        return material;
    }

    /** C = ρ·c·V, J/K. */
    public double heatCapacity() {
        return heatCapacity;
    }

    public double temperatureKelvin() {
        return temperatureKelvin;
    }

    public double temperatureCelsius() {
        return Constants.toCelsius(temperatureKelvin);
    }

    /** Heat stored relative to absolute zero, J. Used by the energy audit. */
    public double storedEnergy() {
        return heatCapacity * temperatureKelvin;
    }

    /**
     * Adds external power for the next step. Power is re-set every step by the
     * world adapter (a fire that went out stops adding), so this is an
     * accumulator that {@link ThermalGrid#step} consumes.
     */
    public void addPower(double watts) {
        if (!Double.isFinite(watts)) throw new IllegalArgumentException("non-finite power");
        this.externalPowerWatts += watts;
    }

    public double externalPowerWatts() {
        return externalPowerWatts;
    }

    void consumePower() {
        externalPowerWatts = 0;
    }

    void setTemperatureKelvin(double kelvin) {
        this.temperatureKelvin = Temperature.requireValid(kelvin);
    }
}
