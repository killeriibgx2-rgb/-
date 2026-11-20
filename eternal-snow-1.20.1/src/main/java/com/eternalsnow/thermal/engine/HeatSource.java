package com.eternalsnow.thermal.engine;

/**
 * A device that adds heat to one grid cell while it is running.
 *
 * <p>Deliberately trivial. The user's decision was to leave vanilla device
 * mechanics untouched: a furnace burns only while it has something to smelt,
 * and how much it smelts does not change. So the engine does not model fuel
 * energy for vanilla devices at all — a running device simply adds its rated
 * power (docs/redesign-3.0-thermal-core.md §7.2).
 *
 * <p>Why not joules: a vanilla furnace burns one coal in 80 seconds. That is
 * about 250 kW of real energy, which would cook a room in ten seconds. Rather
 * than bend physics to fit an 80-second timer, the device's heat output is a
 * property of the device. Honest about the simplification, and simple.
 *
 * <p>The {@code ratedWatts} of a campfire and a furnace differ, which is how
 * "a furnace heats worse than a campfire" is expressed — one smaller number,
 * no formula.
 */
public final class HeatSource {
    private final int cellIndex;
    private final double ratedWatts;
    private final String id;

    public HeatSource(String id, int cellIndex, double ratedWatts) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        if (cellIndex < 0) throw new IllegalArgumentException("cellIndex must be >= 0");
        if (!Double.isFinite(ratedWatts) || ratedWatts < 0) {
            throw new IllegalArgumentException("ratedWatts must be finite and >= 0");
        }
        this.id = id;
        this.cellIndex = cellIndex;
        this.ratedWatts = ratedWatts;
    }

    public String id() {
        return id;
    }

    public int cellIndex() {
        return cellIndex;
    }

    public double ratedWatts() {
        return ratedWatts;
    }

    /**
     * Applies the source to a grid for this step. Call with {@code running =
     * false} for a device that has nothing to do — it then contributes nothing,
     * which is exactly vanilla behaviour.
     */
    public void apply(ThermalGrid grid, boolean running) {
        if (!running) return;
        grid.cell(cellIndex).addPower(ratedWatts);
    }
}
