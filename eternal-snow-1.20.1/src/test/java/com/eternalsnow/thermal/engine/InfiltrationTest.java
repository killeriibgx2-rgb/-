package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Gaps are the reason a house loses heat, and at cryogenic temperatures they
 * dominate everything else. Numbers here are hand-checked against the stack
 * effect formulas in {@link Infiltration}.
 */
public class InfiltrationTest {

    private static final double WARM = Constants.toKelvin(20);
    private static final double FREEZING = Constants.toKelvin(-200);

    @Test
    public void coldAirIsMuchDenserThanWarmAir() {
        double warm = Ambient.airDensity(WARM);
        double cold = Ambient.airDensity(FREEZING);
        assertEquals("air at +20 C", 1.204, warm, 0.005);
        assertEquals("air at -200 C", 4.826, cold, 0.01);
        assertTrue("cold air is roughly four times denser", cold > 3.5 * warm);
    }

    @Test
    public void conductanceMatchesTheOrificeFormula() {
        double area = 0.01;
        double height = 3.0;
        double expected = expectedConductance(area, height);
        assertEquals(expected, Infiltration.conductance(area, height, WARM, FREEZING), 1e-9);
    }

    private double expectedConductance(double area, double height) {
        double rhoIn = Ambient.airDensity(WARM);
        double rhoOut = Ambient.airDensity(FREEZING);
        double pressure = (rhoOut - rhoIn) * Constants.GRAVITY * height;
        double massFlow = Infiltration.DISCHARGE_COEFFICIENT * area
                * Math.sqrt(2.0 * rhoIn * pressure);
        return massFlow * Constants.SPECIFIC_HEAT_AIR;
    }

    @Test
    public void aTenSquareCentimetreGapLeaksAboutTwoKilowatts() {
        // 0.001 m² at 3 m of stack against a 220 K difference: ~2.1 kW.
        double watts = Infiltration.conductance(0.001, 3.0, WARM, FREEZING) * 220;
        assertEquals("expected ~2.1 kW, got " + watts, 2.1, watts / 1000, 0.3);
    }

    @Test
    public void aHundredSquareCentimetreGapLeaksAboutTwentyKilowatts() {
        // Ten times the area, ten times the leak. At cryogenic temperatures this
        // is why a door left open is catastrophic and a 5 kW fire is hopeless.
        double watts = Infiltration.conductance(0.01, 3.0, WARM, FREEZING) * 220;
        assertEquals("expected ~21 kW, got " + watts, 21.3, watts / 1000, 1.0);
    }

    @Test
    public void doublingTheGapDoublesTheLeak() {
        double one = Infiltration.conductance(0.01, 3.0, WARM, FREEZING);
        double two = Infiltration.conductance(0.02, 3.0, WARM, FREEZING);
        assertEquals(one * 2, two, 1e-9);
    }

    @Test
    public void aHigherOpeningLeaksMoreThanASlowOne() {
        // Real stack effect: flow grows with sqrt(height). Same gap, worse place.
        double floor = Infiltration.conductance(0.01, 1.0, WARM, FREEZING);
        double ceiling = Infiltration.conductance(0.01, 6.0, WARM, FREEZING);
        assertTrue("ceiling gap " + ceiling + " should leak more than floor " + floor,
                ceiling > floor);
        assertEquals(ceiling / floor, Math.sqrt(6.0), 1e-9);
    }

    @Test
    public void noStackFlowWhenTheOutsideAirIsNotDenser() {
        // A hot day outside: the room is the dense side, so this model has no
        // flow. Warm-weather buoyancy is a separate regime and is not modelled.
        assertEquals(0.0, Infiltration.conductance(0.01, 3.0, Constants.toKelvin(-200),
                Constants.toKelvin(20)), 1e-12);
    }

    @Test
    public void aSealedRoomHasNoInfiltration() {
        assertEquals(0.0, Infiltration.conductance(0.0, 3.0, WARM, FREEZING), 1e-12);
        assertEquals(0.0, Infiltration.conductance(0.01, 0.0, WARM, FREEZING), 1e-12);
    }

    @Test
    public void aLeakCoolsARoomThatWouldOtherwiseHoldHeat() {
        // The awkward fact this fixes: a sealed timber room holds +20 C against
        // -200 C for months (shell mass 94 MJ/K). A 100 cm² gap dumps ~21 kW
        // straight into the room's ~11 kJ/K of air, which is a time constant of
        // seconds — but the wooden shell then pumps heat back in, so the air
        // settles somewhere in between. What matters is that the gap moves the
        // room a long way towards the outside temperature.
        ThermalGrid sealed = room(0);
        ThermalGrid leaky = room(1);
        run(sealed, 600);
        run(leaky, 600);
        double sealedAir = airMean(sealed);
        double leakyAir = airMean(leaky);
        assertTrue("sealed room should still be warm, got " + sealedAir, sealedAir > 0);
        // The gap does not win outright: the 56 MJ/K wooden shell keeps pumping
        // heat back in, so the air settles around -19 C in this geometry. That
        // balance is the interesting part — a leaky room is cold but not dead.
        assertTrue("a 100 cm2 gap must pull the room far below freezing: sealed " + sealedAir
                        + " vs leaky " + leakyAir,
                leakyAir < -10);
    }

    private ThermalGrid room(int leakApertureHundredths) {
        int n = 5;
        ThermalGrid grid = new ThermalGrid(n, 3, n);
        double start = Constants.toKelvin(20);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < n; z++) {
                for (int x = 0; x < n; x++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == n - 1 || y == 2 || z == n - 1;
                    grid.set(x, y, z, shell ? Materials.WOOD : Materials.AIR, start);
                }
            }
        }
        if (leakApertureHundredths > 0) {
            grid.addInfiltration(new InfiltrationPath(grid.index(2, 1, 2),
                    0.01, 2.0, Ambient.constant(Constants.toKelvin(-200))));
        }
        return grid;
    }

    private void run(ThermalGrid grid, double seconds) {
        for (int i = 0; i < (int) seconds; i++) grid.step(1.0);
    }

    private double airMean(ThermalGrid grid) {
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
}
