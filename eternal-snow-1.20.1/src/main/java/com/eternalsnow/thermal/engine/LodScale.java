package com.eternalsnow.thermal.engine;

/**
 * Level of detail as a physical merge, not an approximation.
 *
 * <p>The rule that makes this safe is "resistance per unit area is preserved".
 * Two cubes of side s sharing a face of area s² have conductance
 * {@code G = λ·s}, so the series resistance between two s-blocks is
 * {@code 2/(λ·s)} over that area — exactly the same per square metre as the
 * chain of 1 m cells it replaced. Verified numerically in
 * {@code LodTest}; merging therefore changes resolution, not physics.
 *
 * @param factor number of base cells per side of a merged cell (1, 2, 4, 8…)
 */
public record LodScale(int factor) {
    public LodScale {
        if (factor < 1) throw new IllegalArgumentException("factor must be >= 1");
        if (Integer.bitCount(factor) != 1) {
            throw new IllegalArgumentException("factor must be a power of two, got " + factor);
        }
    }

    public static final LodScale BASE = new LodScale(1);

    /** Edge length of a merged cell, m. */
    public double cellSize() {
        return factor * Constants.CELL_SIZE;
    }

    /** Heat capacity of a merged cell, J/K. */
    public double heatCapacity(double density, double specificHeat) {
        double volume = Math.pow(cellSize(), 3);
        return density * specificHeat * volume;
    }

    /** Face area between two merged cells, m². */
    public double faceArea() {
        return cellSize() * cellSize();
    }

    /**
     * Interface conductance between two adjacent merged cells of the same
     * material: {@code G = λ · s}. Per unit area this is identical for every
     * factor, which is the invariant the merge relies on.
     */
    public double conductance(Material material) {
        return material.conductivity() * cellSize();
    }

    /**
     * Conductance between two merged cells of different materials, from the
     * series half-thickness resistances over the merged face.
     */
    public double conductance(Material a, Material b) {
        double s = cellSize();
        double halfA = (s / 2.0) / a.conductivity();
        double halfB = (s / 2.0) / b.conductivity();
        return faceArea() / (halfA + halfB);
    }
}
