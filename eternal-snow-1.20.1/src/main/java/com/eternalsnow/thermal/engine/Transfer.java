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
    /** Room-scale circulation exchange speeds between neighbouring air voxels, m/s. */
    private static final double HORIZONTAL_AIR_EXCHANGE_SPEED = 0.4;
    private static final double VERTICAL_AIR_RETURN_SPEED = 0.4;
    private static final double VERTICAL_AIR_UPDRAFT_SPEED = 0.8;

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

    /**
     * Heat exchange from a well-mixed air voxel to the centre of an adjacent
     * solid voxel, W/K. The air-side film and the solid half-cell are in series:
     * coupling the air directly to the full block heat capacity would make a
     * one-metre wooden wall absorb heat as if its whole mass were at the
     * surface temperature.
     */
    public static double airSolidConductance(Material solid) {
        if (solid == null) throw new NullPointerException("solid");
        double area = Constants.FACE_AREA;
        double airFilmResistance = 1.0 / (Constants.FREE_CONVECTION * area);
        double solidHalfCellResistance = (Constants.CELL_SIZE / 2.0)
                / (solid.conductivity() * area);
        return 1.0 / (airFilmResistance + solidHalfCellResistance);
    }

    /**
     * Effective horizontal exchange between adjacent air voxels, W/K. This is
     * room-scale bulk circulation (rho·cp·A·v), not molecular conductivity.
     */
    public static double horizontalAirMixingConductance() {
        return airExchangeConductance(HORIZONTAL_AIR_EXCHANGE_SPEED);
    }

    /**
     * Effective vertical exchange between adjacent air voxels, W/K. A baseline
     * return circulation keeps room air from remaining stratified; a warmer
     * lower cell doubles the exchange speed to model the rising plume.
     */
    public static double verticalAirMixingConductance(double lowerKelvin, double upperKelvin) {
        double speed = lowerKelvin > upperKelvin
                ? VERTICAL_AIR_UPDRAFT_SPEED : VERTICAL_AIR_RETURN_SPEED;
        return airExchangeConductance(speed);
    }

    /** Maximum vertical conductance, used to choose a safe explicit time step. */
    public static double maximumVerticalAirMixingConductance() {
        return airExchangeConductance(VERTICAL_AIR_UPDRAFT_SPEED);
    }

    private static double airExchangeConductance(double speedMetresPerSecond) {
        return Materials.AIR.density() * Constants.SPECIFIC_HEAT_AIR
                * Constants.FACE_AREA * speedMetresPerSecond;
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
