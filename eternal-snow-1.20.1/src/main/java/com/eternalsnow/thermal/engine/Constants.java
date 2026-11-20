package com.eternalsnow.thermal.engine;

/** Physical constants and fixed model parameters, all in SI. */
public final class Constants {
    private Constants() {
    }

    /** Stefan–Boltzmann constant σ, W/(m²·K⁴). */
    public static final double STEFAN_BOLTZMANN = 5.670374419e-8;

    /** Standard gravity, m/s². */
    public static final double GRAVITY = 9.80665;

    /** Zero Celsius on the Kelvin scale. */
    public static final double KELVIN_OFFSET = 273.15;

    /** One Minecraft block is one metre. Volume of a simulation cell, m³. */
    public static final double CELL_VOLUME = 1.0;

    /** Edge length of a simulation cell, m. */
    public static final double CELL_SIZE = 1.0;

    /** Face area between two neighbouring cells, m². */
    public static final double FACE_AREA = 1.0;

    /**
     * Combined surface film resistance for a 1 m wall, m²·K/W.
     * Inside ~1/8 W/(m²·K) plus outside ~1/25 W/(m²·K). Ignoring this makes any
     * highly conductive material look dramatically worse than it is, because
     * real heat loss then becomes limited by the air layer, not the material.
     */
    public static final double SURFACE_FILM_RESISTANCE = 0.165;

    /** Free-convection coefficient from a solid surface to still air, W/(m²·K). */
    public static final double FREE_CONVECTION = 10.0;

    /** Standard atmospheric pressure, Pa. */
    public static final double STANDARD_PRESSURE = 101_325.0;

    /** Specific gas constant of dry air, J/(kg·K). */
    public static final double SPECIFIC_GAS_AIR = 287.05;

    /** Specific heat capacity of air at constant pressure, J/(kg·K). */
    public static final double SPECIFIC_HEAT_AIR = 1005.0;

    /** Seconds in one game tick, at the vanilla 20 ticks per second. */
    public static final double SECONDS_PER_TICK = 0.05;

    public static double toCelsius(double kelvin) {
        return kelvin - KELVIN_OFFSET;
    }

    public static double toKelvin(double celsius) {
        return celsius + KELVIN_OFFSET;
    }
}
