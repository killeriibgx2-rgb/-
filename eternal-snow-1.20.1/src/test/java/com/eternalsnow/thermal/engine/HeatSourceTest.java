package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Devices add their rated power, and only while they are running. */
public class HeatSourceTest {

    private ThermalGrid cell(double celsius) {
        ThermalGrid grid = new ThermalGrid(1, 1, 1);
        grid.set(0, 0, 0, Materials.STONE, Constants.toKelvin(celsius));
        return grid;
    }

    @Test
    public void aRunningCampfireDeliversFiveKilowatts() {
        ThermalGrid grid = cell(0);
        HeatSource fire = new HeatSource("campfire", 0, Devices.CAMPFIRE_WATTS);
        double before = grid.totalEnergy();
        fire.apply(grid, true);
        grid.step(1.0);
        assertEquals(5000, grid.totalEnergy() - before, 1e-6);
    }

    @Test
    public void aDeviceThatHasNothingToDoAddsNothing() {
        // Vanilla behaviour: an idle furnace produces no heat. Not a special
        // case in the engine — simply "running = false".
        ThermalGrid grid = cell(0);
        HeatSource furnace = new HeatSource("furnace", 0, Devices.FURNACE_WATTS);
        double before = grid.totalEnergy();
        furnace.apply(grid, false);
        grid.step(1.0);
        assertEquals(before, grid.totalEnergy(), 1e-9);
    }

    @Test
    public void aFurnaceHeatsFarWorseThanACampfire() {
        assertTrue("furnace must be the weaker heater",
                Devices.FURNACE_WATTS * 3 < Devices.CAMPFIRE_WATTS);
    }

    @Test
    public void powerIsAppliedEveryStepWhileRunning() {
        ThermalGrid grid = cell(0);
        HeatSource fire = new HeatSource("campfire", 0, Devices.CAMPFIRE_WATTS);
        double before = grid.totalEnergy();
        for (int i = 0; i < 10; i++) {
            fire.apply(grid, true);
            grid.step(1.0);
        }
        assertEquals(10 * 5000, grid.totalEnergy() - before, 1e-6);
    }

    @Test
    public void invalidSourcesAreRejected() {
        assertRejected(() -> new HeatSource("", 0, 100));
        assertRejected(() -> new HeatSource("x", -1, 100));
        assertRejected(() -> new HeatSource("x", 0, -100));
        assertRejected(() -> new HeatSource("x", 0, Double.NaN));
    }

    private void assertRejected(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
