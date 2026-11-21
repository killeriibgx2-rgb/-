package com.eternalsnow.thermal.world;

import java.util.ArrayList;
import java.util.List;

/** Finds open two-block Minecraft door openings that separate indoor air from outside air. */
public final class DoorLeakDetector {
    /** One door block spans 1 m² at this grid scale; pressure head is half a 2 m doorway. */
    public static final double AREA_PER_DOOR_BLOCK_M2 = 1.0;
    public static final double DOOR_PRESSURE_HEAD_M = 1.0;

    private DoorLeakDetector() {
    }

    public record Leak(int insideCellIndex, double areaM2, double pressureHeadM) {
    }

    /**
     * Air connected to the zone boundary is outside. Open door cells are
     * treated as barriers during this classification, then each such opening
     * is checked for one outside-facing and one room-facing side.
     *
     * <p>The arrays use the ThermalGrid layout: {@code (y * sizeZ + z) * sizeX + x}.
     */
    public static List<Leak> detect(int sizeX, int sizeY, int sizeZ,
                                    boolean[] airCells, boolean[] openDoorCells) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException("grid dimensions must be positive");
        }
        int count = Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);
        if (airCells.length != count || openDoorCells.length != count) {
            throw new IllegalArgumentException("cell arrays must match grid dimensions");
        }

        boolean[] outside = new boolean[count];
        int[] queue = new int[count];
        int read = 0;
        int write = 0;
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    if (x != 0 && x != sizeX - 1 && y != 0 && y != sizeY - 1
                            && z != 0 && z != sizeZ - 1) continue;
                    int index = index(x, y, z, sizeX, sizeZ);
                    if (airCells[index] && !openDoorCells[index] && !outside[index]) {
                        outside[index] = true;
                        queue[write++] = index;
                    }
                }
            }
        }

        while (read < write) {
            int index = queue[read++];
            int x = index % sizeX;
            int z = (index / sizeX) % sizeZ;
            int y = index / (sizeX * sizeZ);
            if (x > 0) write = visit(index - 1, airCells, openDoorCells, outside, queue, write);
            if (x + 1 < sizeX) write = visit(index + 1, airCells, openDoorCells, outside, queue, write);
            if (y > 0) write = visit(index - sizeX * sizeZ, airCells, openDoorCells, outside, queue, write);
            if (y + 1 < sizeY) write = visit(index + sizeX * sizeZ, airCells, openDoorCells, outside, queue, write);
            if (z > 0) write = visit(index - sizeX, airCells, openDoorCells, outside, queue, write);
            if (z + 1 < sizeZ) write = visit(index + sizeX, airCells, openDoorCells, outside, queue, write);
        }

        List<Leak> leaks = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            if (!openDoorCells[index] || !airCells[index]) continue;
            int x = index % sizeX;
            int z = (index / sizeX) % sizeZ;
            Leak leak = sideLeak(x > 0 ? index - 1 : -1,
                    x + 1 < sizeX ? index + 1 : -1,
                    airCells, openDoorCells, outside);
            if (leak == null) {
                leak = sideLeak(z > 0 ? index - sizeX : -1,
                        z + 1 < sizeZ ? index + sizeX : -1,
                        airCells, openDoorCells, outside);
            }
            if (leak != null) leaks.add(leak);
        }
        return List.copyOf(leaks);
    }

    private static Leak sideLeak(int negative, int positive,
                                 boolean[] airCells, boolean[] openDoorCells,
                                 boolean[] outside) {
        if (negative < 0 || positive < 0
                || !airCells[negative] || !airCells[positive]
                || openDoorCells[negative] || openDoorCells[positive]
                || outside[negative] == outside[positive]) {
            return null;
        }
        int inside = outside[negative] ? positive : negative;
        return new Leak(inside, AREA_PER_DOOR_BLOCK_M2, DOOR_PRESSURE_HEAD_M);
    }

    private static int visit(int index, boolean[] airCells, boolean[] openDoorCells,
                             boolean[] outside, int[] queue, int write) {
        if (airCells[index] && !openDoorCells[index] && !outside[index]) {
            outside[index] = true;
            queue[write++] = index;
        }
        return write;
    }

    private static int index(int x, int y, int z, int sizeX, int sizeZ) {
        return (y * sizeZ + z) * sizeX + x;
    }
}
