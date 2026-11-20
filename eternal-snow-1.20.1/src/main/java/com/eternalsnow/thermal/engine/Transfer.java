package com.eternalsnow.thermal.engine;

/**
 * The three heat transfer channels, in SI.
 *
 * <p>Each method returns a <em>conductance</em> (W/K) rather than a heat flow, so
 * the solver can treat every channel identically and, more importantly, so the
 * two cells sharing a channel see exactly opposite flows. That is what keeps
 * energy conserved; computing each side independently is how models drift.
 */
public final class Transfer {
    private Transfer() {
    }

    /**
     * Conduction between two solid cells across one shared face.
     *
     * <p>Two half-thickness resistances in series:
     * {@code R = d_a/(2λ_a) + d_b/(2λ_b)}, {@code G = A/R}.
     */
    public static double conductionConductance(Material a, Material b) {
        double halfA = (Constants.CELL_SIZE / 2.0) / a.conductivity();
        double halfB = (Constants.CELL_SIZE / 2.0) / b.conductivity();
        return Constants.FACE_AREA / (halfA + halfB);
    }

    /**
     * Conduction through a whole 1 m wall of one material, as the surrounding
     * air sees it: the half-cell resistance plus the surface film on both sides.
     * This is the number a player can check against a real U-value.
     */
    public static double wallTransmittance(Material material) {
        double resistance = Constants.CELL_SIZE / material.conductivity()
                + Constants.SURFACE_FILM_RESISTANCE;
        return 1.0 / resistance;
    }

    /** Free convection between a solid face and still air, W/K. */
    public static double convectionConductance() {
        return Constants.FREE_CONVECTION * Constants.FACE_AREA;
    }

    /**
     * Linearised radiative conductance between two facing surfaces, W/K.
     *
     * <p>Exact exchange is {@code εσA(T_a⁴ − T_b⁴)}. For a step solver we use the
     * factorised form {@code εσA(T_a²+T_b²)(T_a+T_b)ΔT}, which is the exact
     * derivative of that expression and therefore conservative: both cells use
     * the same conductance, so nothing is created or destroyed.
     *
     * <p>This channel is not a detail at cryogenic extremes — for a warm room
     * against a −200 °C wall it dominates conduction
     * (see docs/redesign-3.0-thermal-core.md §3.2).
     */
    public static double radiativeConductance(Material a, Material b,
                                             double tempA, double tempB) {
        double emissivity = 1.0 / (1.0 / a.emissivity() + 1.0 / b.emissivity() - 1.0);
        if (emissivity <= 0) return 0.0;
        double mean = (tempA * tempA + tempB * tempB) * (tempA + tempB);
        return emissivity * Constants.STEFAN_BOLTZMANN * Constants.FACE_AREA * mean;
    }
}
