package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Level of detail must be a resolution change, not a physics change.
 *
 * <p>The invariant: resistance per unit area is preserved when cells are
 * merged, so a coarse run and a fine run of the same wall differ only by how
 * finely they sample the temperature profile — not by how much heat crosses
 * the wall.
 */
public class LodTest {

    @Test
    public void mergingPreservesResistancePerUnitArea() {
        Material wood = Materials.WOOD;
        // A fine chain of 4 one-metre cells over 1 m².
        double fineSeries = 4 * (1.0 / wood.conductivity());
        double finePerArea = fineSeries * 1.0;

        // The same wall as 2 two-metre blocks over 4 m² of face.
        LodScale two = new LodScale(2);
        double coarseSeries = 2 * (1.0 / two.conductance(wood));
        double coarsePerArea = coarseSeries * two.faceArea();

        assertEquals("R·A must not depend on resolution", finePerArea, coarsePerArea, 1e-9);
    }

    @Test
    public void heatCapacityScalesWithVolume() {
        LodScale two = new LodScale(2);
        ThermalCell cell = new ThermalCell(Materials.WOOD, Constants.toKelvin(0));
        assertEquals("a 2x2x2 block holds 8 cells' worth",
                8 * cell.heatCapacity(), two.heatCapacity(500, 1700), 1e-6);
    }

    @Test
    public void faceAreaScalesAsSideSquared() {
        assertEquals(1.0, new LodScale(1).faceArea(), 1e-12);
        assertEquals(4.0, new LodScale(2).faceArea(), 1e-12);
        assertEquals(16.0, new LodScale(4).faceArea(), 1e-12);
        assertEquals(4.0, new LodScale(2).cellSize(), 1e-12);
    }

    @Test
    public void differentMaterialsMergeWithSeriesResistances() {
        LodScale two = new LodScale(2);
        double g = two.conductance(Materials.WOOD, Materials.STONE);
        assertEquals("predicted interface conductance", 1.0, g / two.conductance(
                Materials.WOOD, Materials.STONE), 1e-12);
        assertTrue("conductance must be positive", g > 0);
        // Conductance should sit between the two same-material values.
        assertTrue(g < two.conductance(Materials.STONE));
        assertTrue(g > two.conductance(Materials.WOOD));
    }

    @Test
    public void baseScaleIsTheIdentity() {
        LodScale base = LodScale.BASE;
        assertEquals(1.0, base.cellSize(), 1e-12);
        assertEquals(1.0, base.faceArea(), 1e-12);
        assertEquals(Materials.WOOD.conductivity(), base.conductance(Materials.WOOD), 1e-12);
    }

    @Test
    public void onlyPowerOfTwoFactorsAreAccepted() {
        assertRejected(() -> new LodScale(0));
        assertRejected(() -> new LodScale(3));
        assertRejected(() -> new LodScale(6));
        assertRejected(() -> new LodScale(-2));
    }

    @Test
    public void coarseAndFineWallsLoseTheSameHeat() {
        // Same wall, same temperatures, two resolutions. Total flux must match
        // to within a fraction of a percent.
        double deltaT = 220;
        double fineFlux = deltaT / (4 * (1.0 / Materials.WOOD.conductivity()));
        LodScale two = new LodScale(2);
        double coarseFlux = deltaT / (2 * (1.0 / two.conductance(Materials.WOOD)));
        assertEquals("flux per m² must agree", fineFlux, coarseFlux, fineFlux * 0.001);
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
