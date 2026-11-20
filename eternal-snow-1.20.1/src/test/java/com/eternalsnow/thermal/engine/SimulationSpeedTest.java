package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The speed multiplier must be a time-scaling, not an approximation.
 *
 * <p>Scaling fluxes by N is algebraically the same as integrating N times
 * longer, so the two runs have to agree to within integration error — and the
 * cost per tick must not grow with N, which is the whole point (§5.3).
 */
public class SimulationSpeedTest {

    private ThermalGrid box(double celsius) {
        ThermalGrid grid = new ThermalGrid(3, 3, 3);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == 2 || y == 2 || z == 2;
                    grid.set(x, y, z, Materials.WOOD,
                            Constants.toKelvin(shell ? celsius : celsius));
                }
            }
        }
        grid.cell(1, 1, 1).addPower(2000);
        return grid;
    }

    @Test
    public void speedSixtyEqualsSixtyTimesMoreTime() {
        ThermalGrid fast = box(-200);
        ThermalGrid slow = box(-200);
        fast.setSpeedMultiplier(60);
        slow.setSpeedMultiplier(1);

        // One minute of real time at x60 versus one hour of real time at x1.
        for (int i = 0; i < 60; i++) {
            fast.cell(1, 1, 1).addPower(2000);
            fast.step(1.0);
        }
        for (int i = 0; i < 3600; i++) {
            slow.cell(1, 1, 1).addPower(2000);
            slow.step(1.0);
        }
        assertEquals("x60 for a minute must equal x1 for an hour",
                slow.cell(1, 1, 1).temperatureCelsius(),
                fast.cell(1, 1, 1).temperatureCelsius(),
                0.5);
    }

    @Test
    public void aHigherMultiplierDoesNotCostMoreSubSteps() {
        ThermalGrid fast = box(-200);
        ThermalGrid slow = box(-200);
        fast.setSpeedMultiplier(100);
        slow.setSpeedMultiplier(1);
        int fastSteps = fast.step(1.0);
        int slowSteps = slow.step(1.0);
        assertTrue("x100 used " + fastSteps + " sub-steps vs " + slowSteps
                        + " at x1; speed must be free",
                fastSteps <= slowSteps + 2);
    }

    @Test
    public void zeroSpeedPausesThePhysicsButNotTheWorld() {
        ThermalGrid grid = box(20);
        grid.setSpeedMultiplier(0);
        double before = grid.cell(1, 1, 1).temperatureCelsius();
        for (int i = 0; i < 100; i++) {
            grid.cell(1, 1, 1).addPower(2000);
            grid.step(1.0);
        }
        assertEquals("physics paused", before, grid.cell(1, 1, 1).temperatureCelsius(), 1e-9);
    }

    @Test
    public void theCryogenicDefaultIsSixty() {
        assertEquals(60.0, SimulationClock.cryogenic().speedMultiplier(), 1e-9);
        assertEquals(1.0, SimulationClock.normal().speedMultiplier(), 1e-9);
    }

    @Test
    public void modelSecondsIsJustMultiplication() {
        SimulationClock clock = new SimulationClock(30);
        assertEquals(1800.0, clock.modelSeconds(60), 1e-9);
    }

    @Test
    public void absurdSpeedsAreRejected() {
        assertRejected(() -> new SimulationClock(-1));
        assertRejected(() -> new SimulationClock(SimulationClock.MAX_SPEED * 2));
        assertRejected(() -> new SimulationClock(Double.NaN));
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
