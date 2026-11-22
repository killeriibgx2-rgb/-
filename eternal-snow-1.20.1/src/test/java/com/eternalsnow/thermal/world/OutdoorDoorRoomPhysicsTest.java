package com.eternalsnow.thermal.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.eternalsnow.thermal.engine.Ambient;
import com.eternalsnow.thermal.engine.Constants;
import com.eternalsnow.thermal.engine.Devices;
import com.eternalsnow.thermal.engine.ExternalLink;
import com.eternalsnow.thermal.engine.InfiltrationPath;
import com.eternalsnow.thermal.engine.Material;
import com.eternalsnow.thermal.engine.Materials;
import com.eternalsnow.thermal.engine.ThermalCell;
import com.eternalsnow.thermal.engine.ThermalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Core integration of outdoor-door detection, infiltration and room-air mixing. */
public class OutdoorDoorRoomPhysicsTest {
    private static final int SIZE_X = 9;
    private static final int SIZE_Y = 7;
    private static final int SIZE_Z = 9;
    private static final double OUTSIDE_CELSIUS = -3.0;
    private static final int MODEL_SECONDS = 5 * 60;

    private record Scenario(ThermalGrid grid, List<Integer> roomAirCells, int leakCount) {
    }

    private record AirRange(double minimum, double maximum, double mean) {
    }

    @Test
    public void openOutdoorDoorCoolsTheRoomButAirStillEqualizesAroundTheFire() {
        Scenario sealed = createRoom(false);
        Scenario open = createRoom(true);
        assertEquals("two-block open door should create two aperture paths", 2, open.leakCount());
        assertEquals("sealed wall should not create an outdoor leak", 0, sealed.leakCount());

        runWithCampfire(sealed.grid());
        runWithCampfire(open.grid());

        AirRange sealedAir = airRange(sealed);
        AirRange openAir = airRange(open);
        assertTrue("fire should warm the room above outside, got " + openAir.mean(),
                openAir.mean() > OUTSIDE_CELSIUS);
        assertTrue("open door should cause real heat loss: sealed " + sealedAir.mean()
                        + " C vs open " + openAir.mean() + " C",
                openAir.mean() < sealedAir.mean() - 1.0);
        assertTrue("room air should not split into a hot fire pocket and cold draft: "
                        + openAir.minimum() + " .. " + openAir.maximum() + " C",
                openAir.maximum() - openAir.minimum() <= 3.0);
    }

    private Scenario createRoom(boolean doorOpen) {
        ThermalGrid grid = new ThermalGrid(SIZE_X, SIZE_Y, SIZE_Z);
        double outsideKelvin = Constants.toKelvin(OUTSIDE_CELSIUS);
        Ambient ambient = Ambient.constant(outsideKelvin);
        boolean[] airCells = new boolean[grid.cellCount()];
        boolean[] openDoorCells = new boolean[grid.cellCount()];
        List<Integer> roomAirCells = new ArrayList<>();

        // Start with a solid wooden envelope and carve out a 5x3x5 room.
        for (int y = 0; y < SIZE_Y; y++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int x = 0; x < SIZE_X; x++) {
                    grid.set(x, y, z, Materials.WOOD, outsideKelvin);
                }
            }
        }
        for (int y = 2; y <= 4; y++) {
            for (int z = 2; z <= 6; z++) {
                for (int x = 2; x <= 6; x++) {
                    int index = grid.index(x, y, z);
                    grid.set(x, y, z, Materials.AIR, outsideKelvin);
                    airCells[index] = true;
                    roomAirCells.add(index);
                }
            }
        }

        // A short outdoor-air column reaches the zone boundary outside the door.
        for (int y = 2; y <= 3; y++) {
            int outsideIndex = grid.index(0, y, 4);
            grid.set(0, y, 4, Materials.AIR, outsideKelvin);
            airCells[outsideIndex] = true;

            if (doorOpen) {
                int doorIndex = grid.index(1, y, 4);
                grid.set(1, y, 4, Materials.AIR, outsideKelvin);
                airCells[doorIndex] = true;
                openDoorCells[doorIndex] = true;
            }
        }

        attachOuterBoundary(grid, ambient);
        List<DoorLeakDetector.Leak> leaks = DoorLeakDetector.detect(
                SIZE_X, SIZE_Y, SIZE_Z, airCells, openDoorCells);
        for (DoorLeakDetector.Leak leak : leaks) {
            grid.blockAirMixingThroughCell(leak.openingCellIndex());
            grid.addInfiltration(new InfiltrationPath(leak.insideCellIndex(), leak.areaM2(),
                    leak.pressureHeadM(), ambient));
        }
        return new Scenario(grid, List.copyOf(roomAirCells), leaks.size());
    }

    private void attachOuterBoundary(ThermalGrid grid, Ambient ambient) {
        for (int y = 0; y < SIZE_Y; y++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int x = 0; x < SIZE_X; x++) {
                    if (x != 0 && y != 0 && z != 0
                            && x != SIZE_X - 1 && y != SIZE_Y - 1 && z != SIZE_Z - 1) {
                        continue;
                    }
                    Material material = grid.cell(x, y, z).material();
                    double conductance = material == Materials.AIR
                            ? Constants.FREE_CONVECTION
                            : ExternalLink.outsideSurfaceConductance(material, 1.0, 0.5);
                    grid.addExternalLink(new ExternalLink(grid.index(x, y, z), ambient,
                            1.0 / conductance));
                }
            }
        }
    }

    private void runWithCampfire(ThermalGrid grid) {
        for (int second = 0; second < MODEL_SECONDS; second++) {
            grid.cell(4, 2, 4).addPower(Devices.CAMPFIRE_WATTS);
            grid.step(1.0);
        }
    }

    private AirRange airRange(Scenario scenario) {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        double sum = 0.0;
        for (int index : scenario.roomAirCells()) {
            ThermalCell cell = scenario.grid().cell(index);
            double celsius = cell.temperatureCelsius();
            minimum = Math.min(minimum, celsius);
            maximum = Math.max(maximum, celsius);
            sum += celsius;
        }
        return new AirRange(minimum, maximum, sum / scenario.roomAirCells().size());
    }
}
