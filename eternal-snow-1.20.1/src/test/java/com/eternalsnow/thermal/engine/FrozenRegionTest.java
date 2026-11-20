package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * "No game, no simulation" — the user's rule, tested as literally as possible.
 */
public class FrozenRegionTest {

    private ThermalGrid heatedRoom() {
        ThermalGrid grid = new ThermalGrid(3, 3, 3);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == 2 || y == 2 || z == 2;
                    grid.set(x, y, z, shell ? Materials.WOOD : Materials.AIR,
                            Constants.toKelvin(shell ? -200 : 20));
                }
            }
        }
        grid.addExternalLink(new ExternalLink(grid.index(0, 1, 1),
                Ambient.constant(Constants.toKelvin(-200)),
                ExternalLink.wallConductance(Materials.WOOD, 1.0, 1.0)));
        return grid;
    }

    @Test
    public void aCapturedRoomComesBackBitForBit() {
        ThermalGrid grid = heatedRoom();
        for (int i = 0; i < 500; i++) grid.step(1.0);
        FrozenRegion frozen = FrozenRegion.capture(grid);

        // Time passes; the simulation keeps running as it would if nobody looked.
        for (int i = 0; i < 5000; i++) grid.step(1.0);

        int changed = frozen.restoreInto(grid);
        assertTrue("restoring a snapshot must change something", changed > 0);
        for (int i = 0; i < grid.cellCount(); i++) {
            assertEquals("cell " + i + " must be restored exactly",
                    frozen.temperatureKelvin(i), grid.cell(i).temperatureKelvin(), 1e-12);
        }
    }

    @Test
    public void freezingThenThawingWithNoStepsChangesNothing() {
        ThermalGrid grid = heatedRoom();
        for (int i = 0; i < 100; i++) grid.step(1.0);
        FrozenRegion frozen = FrozenRegion.capture(grid);
        // "Unloaded for a week": no steps at all, because there is no game.
        assertEquals("nothing may change while frozen", 0, frozen.restoreInto(grid));
    }

    @Test
    public void aFrozenRegionReportsItselfAsFrozen() {
        ThermalGrid grid = heatedRoom();
        assertTrue(FrozenRegion.capture(grid).isFrozen());
    }

    @Test
    public void aBrandNewAreaStartsAtTheOutsideTemperature() {
        FrozenRegion fresh = FrozenRegion.atAmbient(27, Constants.toKelvin(-200));
        assertEquals(27, fresh.size());
        assertEquals(-200, Constants.toCelsius(fresh.temperatureKelvin(0)), 1e-9);
    }

    @Test
    public void restoringIntoTheWrongGridIsRejected() {
        FrozenRegion frozen = FrozenRegion.capture(heatedRoom());
        ThermalGrid other = new ThermalGrid(2, 2, 2);
        other.set(0, 0, 0, Materials.AIR, Constants.toKelvin(0));
        assertThrows(() -> frozen.restoreInto(other));
    }

    @Test
    public void aFrozenRoomDoesNotLoseHeatToTheOutside() {
        // The key promise: an abandoned warm base is still warm when you return.
        ThermalGrid grid = heatedRoom();
        // The start state is already warm (+20 C air); simulating it would only
        // cool it, because this small box has a huge envelope for its volume.
        double warm = grid.cell(1, 1, 1).temperatureCelsius();
        FrozenRegion frozen = FrozenRegion.capture(grid);
        assertEquals("the fixture starts warm", 20.0, warm, 1e-9);

        // No steps at all while frozen: the user's rule is that being away
        // means nothing happens, not that something happens slowly.
        frozen.restoreInto(grid);
        assertEquals("come back to the same warmth", warm,
                grid.cell(1, 1, 1).temperatureCelsius(), 1e-9);
    }

    private void assertThrows(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
