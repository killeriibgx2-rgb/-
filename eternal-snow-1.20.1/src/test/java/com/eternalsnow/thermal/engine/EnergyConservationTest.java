package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The one invariant everything else leans on: heat is never created or
 * destroyed inside the grid. If this test fails, every temperature in the mod
 * is fiction.
 */
public class EnergyConservationTest {

    private ThermalGrid twoMaterialBox(double hotCelsius, double coldCelsius) {
        ThermalGrid grid = new ThermalGrid(3, 1, 1);
        grid.set(0, 0, 0, Materials.STONE, Constants.toKelvin(hotCelsius));
        grid.set(1, 0, 0, Materials.STONE, Constants.toKelvin(hotCelsius));
        grid.set(2, 0, 0, Materials.WOOD, Constants.toKelvin(coldCelsius));
        return grid;
    }

    @Test
    public void conductionAloneNeverChangesTotalEnergy() {
        ThermalGrid grid = twoMaterialBox(100, -50);
        double before = grid.totalEnergy();
        for (int i = 0; i < 2000; i++) grid.step(0.25);
        double after = grid.totalEnergy();
        double relative = Math.abs(after - before) / before;
        assertTrue("relative energy drift was " + relative, relative < 1e-12);
    }

    @Test
    public void externalPowerAddsExactlyItsOwnEnergy() {
        ThermalGrid grid = new ThermalGrid(1, 1, 1);
        grid.set(0, 0, 0, Materials.STONE, Constants.toKelvin(0));
        double before = grid.totalEnergy();

        // 5 kW for 60 s = 300 kJ, delivered in 0.25 s slices.
        double watts = 5000;
        double seconds = 0.25;
        int steps = (int) (60 / seconds);
        for (int i = 0; i < steps; i++) {
            grid.cell(0, 0, 0).addPower(watts);
            grid.step(seconds);
        }

        double gained = grid.totalEnergy() - before;
        double expected = watts * steps * seconds;
        assertEquals(expected, gained, Math.abs(expected) * 1e-9);
    }

    @Test
    public void powerIsNotCarriedOverToLaterSteps() {
        ThermalGrid grid = new ThermalGrid(1, 1, 1);
        grid.set(0, 0, 0, Materials.STONE, Constants.toKelvin(0));
        grid.cell(0, 0, 0).addPower(1000);
        grid.step(1.0);
        double afterFirst = grid.totalEnergy();
        grid.step(1.0);
        assertEquals(
                "a fire that went out must stop heating",
                afterFirst,
                grid.totalEnergy(),
                Math.abs(afterFirst) * 1e-12);
    }

    @Test
    public void equilibriumIsReachedAndStaysThere() {
        ThermalGrid grid = new ThermalGrid(4, 1, 1);
        for (int x = 0; x < 4; x++) {
            grid.set(x, 0, 0, Materials.STONE, Constants.toKelvin(x == 0 ? 100 : -100));
        }
        for (int i = 0; i < 20000; i++) grid.step(1.0);
        double mean = grid.meanTemperatureCelsius();
        assertEquals("all cells should share one temperature", 0.0, mean, 0.5);
        assertEquals(
                "spread should collapse",
                0.0,
                grid.cell(0, 0, 0).temperatureCelsius() - grid.cell(3, 0, 0).temperatureCelsius(),
                0.5);
    }

    @Test
    public void stepCountIsReportedAndPositive() {
        ThermalGrid grid = twoMaterialBox(100, -50);
        int substeps = grid.step(1.0);
        assertTrue("expected sub-stepping for steel-fast paths in stone", substeps >= 1);
    }
}
