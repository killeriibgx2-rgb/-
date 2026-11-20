package com.eternalsnow.thermal.engine;

/**
 * Air leaking through gaps, driven by the stack effect.
 *
 * <p>This is not a nice-to-have. Measured on the grid: a sealed 1 m wooden shell
 * holds +20 °C inside a −200 °C world for months, because a cubic metre of
 * timber is a serious heat store. Real houses lose most of their heat through
 * gaps, and without this channel the mod has no tension at all
 * (docs/redesign-3.0-thermal-core.md §3.6a).
 *
 * <p>Physics used, all standard building science:
 * <pre>
 *   Δρ = ρ_out − ρ_in            ideal gas, so cold air is much denser
 *   Δp = Δρ · g · h              stack pressure over the opening height
 *   ṁ  = C_d · A · √(2·ρ·Δp)     orifice flow
 *   G  = ṁ · c_p                 W/K, the conductance to outside air
 * </pre>
 *
 * <p>At −200 °C outside, air is four times denser than a +20 °C room, so a
 * 10 cm² gap already leaks about 2 kW. That is the intended feel: at cryogenic
 * temperatures sealing the envelope matters more than the wall material.
 */
public final class Infiltration {
    private Infiltration() {
    }

    /** Discharge coefficient for a sharp-edged opening, dimensionless. */
    public static final double DISCHARGE_COEFFICIENT = 0.6;

    /**
     * Conductance from room air to outside air, W/K.
     *
     * @param apertureAreaM2 total gap area, m²
     * @param heightM        height of the opening above the neutral axis, m
     * @param insideKelvin   inside air temperature, K
     * @param outsideKelvin  outside air temperature, K
     * @return conductance, W/K; zero when the outside air is not denser
     *         (no stack flow, which is the stable case)
     */
    public static double conductance(double apertureAreaM2, double heightM,
                                     double insideKelvin, double outsideKelvin) {
        if (apertureAreaM2 <= 0 || heightM <= 0) return 0.0;
        double inside = Temperature.requireValid(insideKelvin);
        double outside = Temperature.requireValid(outsideKelvin);
        double rhoInside = Ambient.airDensity(inside);
        double rhoOutside = Ambient.airDensity(outside);
        double pressure = (rhoOutside - rhoInside) * Constants.GRAVITY * heightM;
        if (pressure <= 0) return 0.0;
        double massFlow = DISCHARGE_COEFFICIENT * apertureAreaM2
                * Math.sqrt(2.0 * rhoInside * pressure);
        return massFlow * Constants.SPECIFIC_HEAT_AIR;
    }
}
