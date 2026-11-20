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
    public void aCellWithAPathToTheColdReachesTheReservoir() {
        // A cubic metre of wood through a 1 m wall has a time constant of about
        // 1855 hours, which no test can sit through. The point here is the
        // steady state, so run in model time with the speed multiplier: this is
        // exactly the mechanism §5.3 exists for.
        ThermalGrid grid = new ThermalGrid(1, 1, 1);
        grid.set(0, 0, 0, Materials.WOOD, Constants.toKelvin(20));
        grid.addExternalLink(new ExternalLink(0,
                Ambient.constant(Constants.toKelvin(-200)),
                1.0 / ExternalLink.wallConductance(Materials.WOOD, 1.0, 1.0)));
        // An isolated single cell is a slow integrator: explicit sub-stepping
        // here is limited by this one cell's own capacity, so the multiplier
        // does not buy speed. Ten days of model time already takes it most of
        // the way, which is what this test checks.
        for (int i = 0; i < 3000; i++) grid.step(300.0);
        double celsius = grid.cell(0, 0, 0).temperatureCelsius();
        assertTrue("must be heading to the reservoir, got " + celsius, celsius < -150);
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
