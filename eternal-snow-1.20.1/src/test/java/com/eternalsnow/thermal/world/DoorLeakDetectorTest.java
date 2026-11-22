package com.eternalsnow.thermal.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** Geometry-only tests; no Minecraft runtime needed. */
public class DoorLeakDetectorTest {
    private static final int X = 5;
    private static final int Y = 4;
    private static final int Z = 5;

    @Test
    public void detectsOnlyTheIndoorSideOfAnOutsideDoor() {
        boolean[] air = new boolean[X * Y * Z];
        boolean[] doors = new boolean[air.length];
        int door = index(1, 2, 2);
        int outsideAir = index(0, 2, 2);
        int roomAir = index(2, 2, 2);
        air[door] = true;
        doors[door] = true;
        air[outsideAir] = true;
        air[roomAir] = true;

        List<DoorLeakDetector.Leak> leaks = DoorLeakDetector.detect(X, Y, Z, air, doors);

        assertEquals(1, leaks.size());
        assertEquals(door, leaks.get(0).openingCellIndex());
        assertEquals(roomAir, leaks.get(0).insideCellIndex());
        assertEquals(1.0, leaks.get(0).areaM2(), 0.0);
        assertEquals(1.0, leaks.get(0).pressureHeadM(), 0.0);
    }

    @Test
    public void aTwoBlockDoorContributesTwoSquareMetres() {
        boolean[] air = new boolean[X * Y * Z];
        boolean[] doors = new boolean[air.length];
        for (int y = 1; y <= 2; y++) {
            air[index(0, y, 2)] = true;
            air[index(1, y, 2)] = true;
            doors[index(1, y, 2)] = true;
            air[index(2, y, 2)] = true;
        }

        List<DoorLeakDetector.Leak> leaks = DoorLeakDetector.detect(X, Y, Z, air, doors);

        assertEquals(2, leaks.size());
        assertEquals(2.0, leaks.stream().mapToDouble(DoorLeakDetector.Leak::areaM2).sum(), 0.0);
        assertEquals(List.of(index(1, 1, 2), index(1, 2, 2)),
                leaks.stream().map(DoorLeakDetector.Leak::openingCellIndex).toList());
        assertEquals(List.of(index(2, 1, 2), index(2, 2, 2)),
                leaks.stream().map(DoorLeakDetector.Leak::insideCellIndex).toList());
    }

    @Test
    public void aDoorBetweenTwoOutdoorAirVolumesIsNotAnIndoorLeak() {
        boolean[] air = new boolean[X * Y * Z];
        boolean[] doors = new boolean[air.length];
        int y = 1;
        int z = 2;
        for (int x = 0; x < X; x++) {
            if (x == 2) {
                air[index(x, y, z)] = true;
                doors[index(x, y, z)] = true;
            } else {
                air[index(x, y, z)] = true;
            }
        }
        assertTrue(DoorLeakDetector.detect(X, Y, Z, air, doors).isEmpty());
    }

    @Test
    public void interiorOrClosedDoorDoesNotBecomeAnOutdoorLeak() {
        boolean[] air = new boolean[X * Y * Z];
        boolean[] doors = new boolean[air.length];
        int door = index(2, 2, 2);
        // Both room sides remain disconnected from the boundary.
        air[index(1, 2, 2)] = true;
        air[index(3, 2, 2)] = true;
        air[door] = true;
        doors[door] = true;
        assertTrue(DoorLeakDetector.detect(X, Y, Z, air, doors).isEmpty());

        // Closed leaves are not open-door apertures and do not add a leak path.
        doors[door] = false;
        air[door] = false;
        air[index(0, 2, 2)] = true;
        assertTrue(DoorLeakDetector.detect(X, Y, Z, air, doors).isEmpty());
    }

    private static int index(int x, int y, int z) {
        return (y * Z + z) * X + x;
    }
}
