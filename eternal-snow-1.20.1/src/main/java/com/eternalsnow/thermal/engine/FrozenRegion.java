package com.eternalsnow.thermal.engine;

import java.util.Arrays;

/**
 * A zone that is not being simulated right now.
 *
 * <p>User decision: no game, no simulation. When nobody is nearby the world is
 * frozen — not "cooling by an exponential", not "catching up later". Loading
 * restores exactly the stored state, so there is no drift, no seam and no
 * surprise temperature jump on the border
 * (docs/redesign-3.0-thermal-core.md §6.1).
 *
 * <p>This is the cheapest possible design and the most predictable one. The
 * exponential catch-up remains available behind a config flag for servers that
 * want a living world, but it is not the default and not in this class.
 *
 * <p>Fuel follows the same rule: it does not burn while frozen. That is
 * consistent — a furnace that produces no heat cannot be consuming fuel.
 */
public final class FrozenRegion {
    private final double[] temperaturesKelvin;
    private final boolean frozen;

    private FrozenRegion(double[] temperaturesKelvin, boolean frozen) {
        this.temperaturesKelvin = temperaturesKelvin;
        this.frozen = frozen;
    }

    /** Captures the current state of a grid. */
    public static FrozenRegion capture(ThermalGrid grid) {
        double[] snapshot = new double[grid.cellCount()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = grid.cell(i).temperatureKelvin();
        }
        return new FrozenRegion(snapshot, true);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int size() {
        return temperaturesKelvin.length;
    }

    /** Temperature stored for a cell, K. */
    public double temperatureKelvin(int index) {
        return temperaturesKelvin[index];
    }

    /**
     * Restores a captured state into a grid. Returns the number of cells whose
     * temperature differed, which is zero for an undisturbed freeze/thaw cycle.
     */
    public int restoreInto(ThermalGrid grid) {
        if (temperaturesKelvin.length != grid.cellCount()) {
            throw new IllegalArgumentException("snapshot does not match this grid");
        }
        int changed = 0;
        for (int i = 0; i < temperaturesKelvin.length; i++) {
            double now = grid.cell(i).temperatureKelvin();
            if (now != temperaturesKelvin[i]) {
                changed++;
                grid.cell(i).setTemperatureKelvin(temperaturesKelvin[i]);
            }
        }
        return changed;
    }

    /** A frozen region restarted from scratch: everything at the outside temperature. */
    public static FrozenRegion atAmbient(int cellCount, double ambientKelvin) {
        double t = Temperature.requireValid(ambientKelvin);
        double[] values = new double[cellCount];
        Arrays.fill(values, t);
        return new FrozenRegion(values, true);
    }
}
