package com.eternalsnow.thermal.engine;

/**
 * A real building material. All values are SI and come from building-physics
 * tables (see docs/redesign-3.0-thermal-core.md §3.3), never from taste.
 *
 * @param id            stable name, used in configs and tests
 * @param conductivity  thermal conductivity λ, W/(m·K)
 * @param density       bulk density ρ, kg/m³
 * @param heatCapacity  specific heat c, J/(kg·K)
 * @param emissivity    long-wave emissivity ε, 0..1
 */
public record Material(
        String id,
        double conductivity,
        double density,
        double heatCapacity,
        double emissivity) {

    public Material {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        requirePositive(conductivity, "conductivity");
        requirePositive(density, "density");
        requirePositive(heatCapacity, "heatCapacity");
        if (emissivity < 0 || emissivity > 1) {
            throw new IllegalArgumentException("emissivity must be within 0..1, got " + emissivity);
        }
    }

    private static void requirePositive(double value, String name) {
        if (!(value > 0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and > 0, got " + value);
        }
    }

    /** Volumetric heat capacity ρ·c, J/(m³·K). */
    public double volumetricHeatCapacity() {
        return density * heatCapacity;
    }

    /**
     * Heat capacity of a 1 m³ cell, J/K. Minecraft cells are 1×1×1 m, so this is
     * also the per-block value.
     */
    public double cellHeatCapacity() {
        return volumetricHeatCapacity() * Constants.CELL_VOLUME;
    }
}
