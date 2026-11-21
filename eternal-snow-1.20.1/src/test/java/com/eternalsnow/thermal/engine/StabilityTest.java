package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Sub-stepping must keep even the worst material combination sane. */
public class StabilityTest {

    @Test
    public void steelNextToAirDoesNotExplode() {
        ThermalGrid grid = new ThermalGrid(2, 1, 1);
        grid.set(0, 0, 0, Materials.STEEL, Constants.toKelvin(500));
        grid.set(1, 0, 0, Materials.AIR, Constants.toKelvin(-200));
        double before = grid.totalEnergy();
        grid.step(1.0);
        for (int i = 0; i < 100; i++) grid.step(1.0);
        double after = grid.totalEnergy();
        assertTrue("energy drifted", Math.abs(after - before) / before < 1e-9);
        assertTrue("temperature exploded", grid.cell(0, 0, 0).temperatureCelsius() < 500);
    }

    @Test
    public void stableStepShrinksWithHighlyConductiveCells() {
        ThermalGrid slow = new ThermalGrid(2, 1, 1);
        slow.set(0, 0, 0, Materials.WOOL, Constants.toKelvin(0));
        slow.set(1, 0, 0, Materials.WOOL, Constants.toKelvin(0));

        ThermalGrid fast = new ThermalGrid(2, 1, 1);
        fast.set(0, 0, 0, Materials.STEEL, Constants.toKelvin(0));
        fast.set(1, 0, 0, Materials.STEEL, Constants.toKelvin(0));

        assertTrue("steel needs a much smaller step", fast.stableStep() < slow.stableStep() / 10);
    }
}
