package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Behaviour of a heated room, pinned to docs/redesign-3.0-thermal-core.md §3.5.
 *
 * <p>Three lessons are baked in, each learned from a failing build:
 *
 * <ol>
 *   <li>A room whose walls are all clamped to the outside temperature cannot
 *       tell materials apart — heat leaves through the clamps, not the
 *       material. The inner layer has to stay free.</li>
 *   <li>Wall mass dominates. Lifting a 1 m shell by 210 K at 5 kW takes hours
 *       (43 h for wool, 2500 h for stone), so a "warm in ten minutes" test is
 *       testing nothing but its own impatience.</li>
 *   <li>Speed of response and steady-state ceiling are different physics:
 *       air capacity vs U·A. Tests here cover both separately.</li>
 * </ol>
 */
public class RoomPhysicsTest {

    private static final int NX = 9, NY = 7, NZ = 9;

    /** Inner 5x3x5 air volume, 1 m free wall, 1 m outer wall fixed to the world. */
    private ThermalGrid room(Material wall, double startCelsius) {
        ThermalGrid grid = new ThermalGrid(NX, NY, NZ);
        double k = Constants.toKelvin(startCelsius);
        for (int y = 0; y < NY; y++) {
            for (int z = 0; z < NZ; z++) {
                for (int x = 0; x < NX; x++) {
                    grid.set(x, y, z, shellDepth(x, y, z) <= 1 ? wall : Materials.AIR, k);
                }
            }
        }
        return grid;
    }

    private int shellDepth(int x, int y, int z) {
        int d = Math.min(Math.min(x, NX - 1 - x), Math.min(y, NY - 1 - y));
        return Math.min(d, Math.min(z, NZ - 1 - z));
    }

    /** The world outside: the outermost layer is a reservoir, not a free cell. */
    private void clampWorld(ThermalGrid grid, double outsideCelsius) {
        double k = Constants.toKelvin(outsideCelsius);
        for (int y = 0; y < NY; y++) {
            for (int z = 0; z < NZ; z++) {
                for (int x = 0; x < NX; x++) {
                    if (shellDepth(x, y, z) == 0) grid.cell(x, y, z).setTemperatureKelvin(k);
                }
            }
        }
    }

    private double airMeanCelsius(ThermalGrid grid) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < grid.cellCount(); i++) {
            ThermalCell cell = grid.cell(i);
            if (cell.material() != Materials.AIR) continue;
            sum += cell.temperatureCelsius();
            count++;
        }
        return sum / count;
    }

    private double minimumAirCelsius(ThermalGrid grid) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < grid.cellCount(); i++) {
            ThermalCell cell = grid.cell(i);
            if (cell.material() == Materials.AIR) {
                min = Math.min(min, cell.temperatureCelsius());
            }
        }
        return min;
    }

    /** Runs {@code seconds} of model time with a constant source in the floor cell. */
    private void run(ThermalGrid grid, Material wall, double seconds, double watts) {
        int steps = (int) Math.ceil(seconds);
        for (int i = 0; i < steps; i++) {
            clampWorld(grid, -200);
            if (watts != 0) grid.cell(4, 3, 4).addPower(watts);
            grid.step(1.0);
        }
    }

    @Test
    public void aFrozenRoomMovesUpWithinTheFirstMinutes() {
        // The air responds first; the shell holds it back. After 10 minutes the
        // air must be measurably above the shell, whatever the wall material.
        for (Material wall : new Material[]{Materials.WOOL, Materials.WOOD,
                Materials.BRICK, Materials.STONE}) {
            ThermalGrid grid = room(wall, -200);
            run(grid, wall, 600, 5000);
            double air = airMeanCelsius(grid);
            assertTrue(wall.id() + " air should be warming, got " + air, air > -199.5);
        }
    }

    @Test
    public void aCampfireWarmsAirAcrossASealedWoodenRoom() {
        ThermalGrid grid = room(Materials.WOOD, -3);

        // The room's air volume is 5x3x5 cells. Heat the lowest central voxel,
        // as a campfire on the floor would, while the outside stays at -3 C.
        for (int second = 0; second < 5 * 60; second++) {
            clampWorld(grid, -3);
            grid.cell(4, 2, 4).addPower(Devices.CAMPFIRE_WATTS);
            grid.step(1.0);
        }

        double aboveFire = grid.cell(4, 3, 4).temperatureCelsius();
        double besideFire = grid.cell(4, 2, 5).temperatureCelsius();
        double coldestRoomAir = minimumAirCelsius(grid);
        assertTrue("the updraft must warm the air over the fire, got " + aboveFire,
                aboveFire > 20);
        assertTrue("heat must spread at least one block sideways, got " + besideFire,
                besideFire > 5);
        assertTrue("the sealed room must not remain at outside temperature; coldest air was "
                        + coldestRoomAir,
                coldestRoomAir > 0);
    }

    @Test
    public void woolIsTheOnlyShellThatWarmsWithinHours() {
        // §3.5: 43 h for wool, >1000 h for wood, brick and stone at 5 kW.
        ThermalGrid wool = room(Materials.WOOL, -200);
        ThermalGrid stone = room(Materials.STONE, -200);
        run(wool, Materials.WOOL, 6 * 3600, 5000);
        run(stone, Materials.STONE, 6 * 3600, 5000);
        double woolAir = airMeanCelsius(wool);
        double stoneAir = airMeanCelsius(stone);
        assertTrue("wool should be far ahead after 6 h: " + woolAir + " vs " + stoneAir,
                woolAir > stoneAir + 1.0);
    }

    @Test
    public void stoneShellCannotBeWarmedByFiveKilowattsAtAll() {
        // Not a bug: it is the mass. 46.6 GJ of stone needs 2587 h at 5 kW.
        ThermalGrid grid = room(Materials.STONE, -200);
        run(grid, Materials.STONE, 12 * 3600, 5000);
        assertTrue("stone air stays near the shell, got " + airMeanCelsius(grid),
                airMeanCelsius(grid) < -190);
    }

    @Test
    public void aSealedWoodenRoomCoolsSlowlyBecauseTheShellIsHeavy() {
        // Deliberately asserts the awkward truth: with no drafts, a 1 m wooden
        // shell holds 94 MJ/K, and its time constant against the clamped outer
        // layer is months. Air therefore stays near the shell for a long time.
        //
        // This is correct physics for a *sealed* room and the reason
        // infiltration (spec §3.7) is not optional: in reality a house loses
        // most of its heat through gaps, not through a metre of timber.
        ThermalGrid grid = room(Materials.WOOD, 20);
        double start = airMeanCelsius(grid);
        run(grid, Materials.WOOD, 24 * 3600, 0);
        double air = airMeanCelsius(grid);
        assertTrue("should have cooled at least a little: " + start + " -> " + air, air < start);
        assertTrue("but not instantly: a heavy shell protects for a long time, got " + air,
                air > -100);
    }

    @Test
    public void noChannelEverFightsItself() {
        // A room left alone with no source must cool monotonically. If any
        // channel had the wrong sign, this catches it immediately.
        ThermalGrid grid = room(Materials.WOOD, 20);
        double previous = airMeanCelsius(grid);
        for (int minute = 0; minute < 60; minute++) {
            run(grid, Materials.WOOD, 60, 0);
            double now = airMeanCelsius(grid);
            assertTrue("temperature went up while cooling: " + previous + " -> " + now,
                    now <= previous + 1e-6);
            previous = now;
        }
    }
}
