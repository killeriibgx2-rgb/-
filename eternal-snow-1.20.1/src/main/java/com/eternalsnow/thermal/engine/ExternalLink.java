package com.eternalsnow.thermal.engine;

/**
 * A conductive path from a cell to something that is not in the grid: the sky,
 * a buried cold sink, a neighbouring chunk that is currently frozen.
 *
 * <p>The far end is held at a fixed temperature, exactly like the clamped outer
 * layer in the tests — it is a reservoir, not a cell. Both the resistance and
 * the reservoir come from physics; the only modelling choice is where the
 * reservoir boundary sits.
 */
public final class ExternalLink {
    private final int cellIndex;
    private final Ambient ambient;
    private final double resistance;

    /**
     * @param cellIndex index of the grid cell on the inside
     * @param ambient   the reservoir on the outside
     * @param resistance K/W, as seen by the cell (includes the surface film)
     */
    public ExternalLink(int cellIndex, Ambient ambient, double resistance) {
        if (cellIndex < 0) throw new IllegalArgumentException("cellIndex must be >= 0");
        if (ambient == null) throw new NullPointerException("ambient");
        if (!(resistance > 0) || !Double.isFinite(resistance)) {
            throw new IllegalArgumentException("resistance must be finite and > 0");
        }
        this.cellIndex = cellIndex;
        this.ambient = ambient;
        this.resistance = resistance;
    }

    public int cellIndex() {
        return cellIndex;
    }

    public double conductance() {
        return 1.0 / resistance;
    }

    public Ambient ambient() {
        return ambient;
    }

    /**
     * Conductance of a whole wall layer of one material, W/K, including the
     * surface film on both sides — the number a player can compare to a real
     * U-value.
     */
    public static double wallConductance(Material material, double areaM2, double thicknessM) {
        return conductance(material, areaM2, thicknessM, Constants.SURFACE_FILM_RESISTANCE);
    }

    /** Conductance from a cell centre to outside air: half-cell plus outside film. */
    public static double outsideSurfaceConductance(Material material, double areaM2,
                                                   double materialPathM) {
        return conductance(material, areaM2, materialPathM,
                Constants.OUTSIDE_SURFACE_FILM_RESISTANCE);
    }

    private static double conductance(Material material, double areaM2, double thicknessM,
                                      double filmResistance) {
        if (material == null) throw new NullPointerException("material");
        if (!(areaM2 > 0) || !Double.isFinite(areaM2)) {
            throw new IllegalArgumentException("area must be finite and > 0");
        }
        if (!(thicknessM > 0) || !Double.isFinite(thicknessM)) {
            throw new IllegalArgumentException("thickness must be finite and > 0");
        }
        double resistance = thicknessM / material.conductivity() + filmResistance;
        return areaM2 / resistance;
    }
}
