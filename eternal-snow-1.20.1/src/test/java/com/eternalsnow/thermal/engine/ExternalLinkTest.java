package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Heat leaving through a wall to the sky, and the wall U-value it implies. */
public class ExternalLinkTest {

    @Test
    public void aWoodenWallLosesTheCalculatedAmount() {
        double conductance = ExternalLink.wallConductance(Materials.WOOD, 110, 1.0);
        // §3.4: 110 m² of 1 m wood at 220 K difference is 3.08 kW.
        assertEquals(3.08, conductance * 220 / 1000, 0.05);
    }

    @Test
    public void insulationBeatsStructureByAnOrderOfMagnitude() {
        double wool = ExternalLink.wallConductance(Materials.WOOL, 110, 1.0);
        double stone = ExternalLink.wallConductance(Materials.STONE, 110, 1.0);
        assertTrue("wool " + wool + " vs stone " + stone, wool * 20 < stone);
    }

    @Test
    public void aCellConnectedToAColdReservoirCoolsAndStaysAboveAbsoluteZero() {
        ThermalGrid grid = new ThermalGrid(1, 1, 1);
        grid.set(0, 0, 0, Materials.WOOD, Constants.toKelvin(20));
        grid.addExternalLink(new ExternalLink(0,
                Ambient.constant(Constants.toKelvin(-200)),
                1.0 / ExternalLink.wallConductance(Materials.WOOD, 1.0, 1.0)));
        // Wood has C = 850 kJ/K and this wall only 6.9 W/K, so the time constant
        // is about 3.4 hours. Five time constants is ~2.4e6 s.
        for (int i = 0; i < 2_500_000; i++) grid.step(1.0);
        double celsius = grid.cell(0, 0, 0).temperatureCelsius();
        assertEquals("must settle on the reservoir", -200, celsius, 1.0);
        assertTrue("never below absolute zero", celsius > -273);
    }

    @Test
    public void aThickerWallLeaksLess() {
        double thin = ExternalLink.wallConductance(Materials.WOOD, 1.0, 0.25);
        double thick = ExternalLink.wallConductance(Materials.WOOD, 1.0, 3.0);
        assertTrue(thick < thin / 2);
    }

    @Test
    public void invalidLinkArgumentsAreRejected() {
        assertRejected(() -> new ExternalLink(-1, Ambient.constant(100), 1));
        assertRejected(() -> new ExternalLink(0, Ambient.constant(100), 0));
        assertRejected(() -> new ExternalLink(0, Ambient.constant(100), Double.NaN));
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
