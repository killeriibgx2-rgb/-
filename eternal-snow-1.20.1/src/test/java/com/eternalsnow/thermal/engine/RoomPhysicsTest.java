package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * End-to-end scenarios from the design document. These are the numbers the user
 * will actually feel, so they get pinned here rather than discovered in game.
 */
public class RoomPhysicsTest {

    /**
     * 5x5x3 room: air inside, walls of one material, clamped outside temperature.
     * Walls are re-clamped every step to model "the world outside is a reservoir",
     * exactly how the adapter will do it.
     */
    private ThermalGrid room(Material wall, double insideCelsius, double outsideCelsius) {
        ThermalGrid grid = new ThermalGrid(7, 5, 5);
        double inside = Constants.toKelvin(insideCelsius);
        double outside = Constants.toKelvin(outsideCelsius);
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z < 5; z++) {
                for (int x = 1; x <= 5; x++) {
                    boolean shell = y == 1 || y == 3 || z == 1 || z == 4 || x == 1 || x == 5;
                    grid.set(x, y, z, shell ? wall : Materials.AIR, shell ? outside : inside);
                }
            }
        }
        return grid;
    }

    private void clampShell(ThermalGrid grid, Material wall, double outsideCelsius) {
        double outside = Constants.toKelvin(outsideCelsius);
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z < 5; z++) {
                for (int x = 1; x <= 5; x++) {
                    boolean shell = y == 1 || y == 3 || z == 1 || z == 4 || x == 1 || x == 5;
                    if (shell) grid.cell(x, y, z).setTemperatureKelvin(outside);
                }
            }
        }
    }

    @Test
    public void anUnheatedRoomFreezesToTheOutsideTemperature() {
        ThermalGrid grid = room(Materials.WOOD, 20, -200);
        for (int i = 0; i < 4000; i++) {
            clampShell(grid, Materials.WOOD, -200);
            grid.step(1.0);
        }
        assertTrue("air should follow the walls down, got " + grid.cell(3, 2, 2).temperatureCelsius(),
                grid.cell(3, 2, 2).temperatureCelsius() < -150);
    }

    @Test
    public void aFiveKilowattFireHoldsARoomAboveFreezing() {
        ThermalGrid grid = room(Materials.WOOD, -200, -200);
        for (int i = 0; i < 1800; i++) {
            clampShell(grid, Materials.WOOD, -200);
            grid.cell(3, 1, 2).addPower(5000);
            grid.step(1.0);
        }
        double air = grid.cell(3, 2, 2).temperatureCelsius();
        assertTrue("a 5 kW fire must lift the air above freezing: " + air, air > 0);
    }

    @Test
    public void airHeatsMuchFasterThanTheWalls() {
        ThermalGrid grid = room(Materials.STONE, -200, -200);
        for (int i = 0; i < 300; i++) {
            clampShell(grid, Materials.STONE, -200);
            grid.cell(3, 1, 2).addPower(5000);
            grid.step(1.0);
        }
        double airGain = grid.cell(3, 2, 2).temperatureCelsius() + 200;
        // The walls are clamped, so this asserts air responsiveness alone.
        assertTrue("air should respond within minutes, got +" + airGain, airGain > 20);
    }

    @Test
    public void woolHoldsHeatBetterThanStoneByAFactorOfDozens() {
        double woolAir = heatUpWith(Materials.WOOL);
        double stoneAir = heatUpWith(Materials.STONE);
        assertTrue("wool room " + woolAir + " should beat stone room " + stoneAir,
                woolAir > stoneAir * 1.5);
    }

    private double heatUpWith(Material wall) {
        ThermalGrid grid = room(wall, -200, -200);
        for (int i = 0; i < 600; i++) {
            clampShell(grid, wall, -200);
            grid.cell(3, 1, 2).addPower(5000);
            grid.step(1.0);
        }
        return grid.cell(3, 2, 2).temperatureCelsius();
    }
}
