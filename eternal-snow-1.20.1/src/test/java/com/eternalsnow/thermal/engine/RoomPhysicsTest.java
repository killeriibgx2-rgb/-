package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * End-to-end behaviour of a heated room, pinned to the numbers in
 * docs/redesign-3.0-thermal-core.md §3.5.
 *
 * <p>Two lessons are baked into these tests, both learned the hard way:
 *
 * <ol>
 *   <li>A room whose walls are all clamped to the outside temperature cannot
 *       tell materials apart — heat leaves through the clamps, not the
 *       material. Only the <em>inner</em> layer may be left free to conduct
 *       into a clamped outer layer, which is how a real wall behaves.</li>
 *   <li>Steady state is governed by U·A, while speed of response is governed by
 *       the air's heat capacity. Testing only "is it warm after N minutes"
 *       hides the material entirely.</li>
 * </ol>
 */
public class RoomPhysicsTest {

    /** Inner 5x3x5 air volume, 1 m free wall, 1 m clamped wall outside that. */
    private ThermalGrid room(Material wall, double startCelsius) {
        int nx = 9, ny = 7, nz = 9;
        ThermalGrid grid = new ThermalGrid(nx, ny, nz);
        double k = Constants.toKelvin(startCelsius);
        for (int y = 0; y < ny; y++) {
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    int d = Math.min(Math.min(x, nx - 1 - x), Math.min(y, ny - 1 - y));
                    d = Math.min(d, Math.min(z, nz - 1 - z));
                    grid.set(x, y, z, d <= 1 ? wall : Materials.AIR, k);
                }
            }
        }
        return grid;
    }

    private void clampOuterShell(ThermalGrid grid, Material wall, double outsideCelsius) {
        int nx = grid.sizeX(), ny = grid.sizeY(), nz = grid.sizeZ();
        double k = Constants.toKelvin(outsideCelsius);
        for (int y = 0; y < ny; y++) {
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    int d = Math.min(Math.min(x, nx - 1 - x), Math.min(y, ny - 1 - y));
                    d = Math.min(d, Math.min(z, nz - 1 - z));
                    if (d == 0) grid.cell(x, y, z).setTemperatureKelvin(k);
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

    private double heatFor(ThermalGrid grid, Material wall, int minutes, double watts) {
        for (int i = 0; i < minutes * 60; i++) {
            clampOuterShell(grid, wall, -200);
            grid.cell(4, 3, 4).addPower(watts);
            grid.step(1.0);
        }
        return airMeanCelsius(grid);
    }

    @Test
    public void fiveKilowattsLiftsACompletelyFrozenRoomInTenMinutes() {
        // §3.5: +21..+33 C after 10 minutes, whichever the wall material.
        for (Material wall : new Material[]{Materials.WOOL, Materials.WOOD,
                Materials.BRICK, Materials.STONE}) {
            ThermalGrid grid = room(wall, -200);
            double air = heatFor(grid, wall, 10, 5000);
            assertTrue(wall.id() + " should be above -170 C after 10 min, got " + air,
                    air > -170);
            assertTrue(wall.id() + " should not exceed +40 C in 10 min, got " + air,
                    air < 40);
        }
    }

    @Test
    public void stoneRoomCeilingIsAboutThirtyDegrees() {
        // §3.5: a 5 kW fire in a 1 m stone shell tops out near +34 C at -200 C.
        ThermalGrid grid = room(Materials.STONE, -200);
        double air = heatFor(grid, Materials.STONE, 90, 5000);
        assertEquals("stone steady state", 34, air, 6);
    }

    @Test
    public void woolRoomKeepsClimbingWhereStoneHasStopped() {
        // The whole point of the material progression: wool does not just heat
        // faster, it has a far higher ceiling.
        ThermalGrid stone = room(Materials.STONE, -200);
        ThermalGrid wool = room(Materials.WOOL, -200);
        double stoneAir = heatFor(stone, Materials.STONE, 180, 5000);
        double woolAir = heatFor(wool, Materials.WOOL, 180, 5000);
        assertTrue("wool " + woolAir + " should beat stone " + stoneAir + " by a wide margin",
                woolAir > stoneAir + 10);
    }

    @Test
    public void anUnheatedRoomFallsToTheOutsideTemperature() {
        ThermalGrid grid = room(Materials.WOOD, 20);
        for (int i = 0; i < 7200; i++) {
            clampOuterShell(grid, Materials.WOOD, -200);
            grid.step(1.0);
        }
        double air = airMeanCelsius(grid);
        assertTrue("an unheated room must freeze, got " + air, air < -150);
    }

    @Test
    public void coolingIsNeverFasterThanHeatingForTheSamePower() {
        // Not a physics law, but a sanity check that no channel flips sign:
        // a symmetric box heated and cooled by the same power must move
        // symmetrically.
        ThermalGrid up = room(Materials.WOOD, -100);
        ThermalGrid down = room(Materials.WOOD, -100);
        double upAir = heatFor(up, Materials.WOOD, 30, 5000);
        for (int i = 0; i < 30 * 60; i++) {
            clampOuterShell(down, Materials.WOOD, -200);
            down.cell(4, 3, 4).addPower(-5000);
            down.step(1.0);
        }
        double downAir = airMeanCelsius(down);
        assertEquals("symmetry", -100 - (upAir + 100), downAir + 100, 5.0);
    }
}
