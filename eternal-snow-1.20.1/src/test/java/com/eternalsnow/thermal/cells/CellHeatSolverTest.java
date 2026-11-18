package com.eternalsnow.thermal.cells;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.eternalsnow.thermal.cells.CellHeatSolver.Edge;

public class CellHeatSolverTest {
    private CellHeatSolver pair(double ca, double cb) {
        return new CellHeatSolver(new double[]{80, -40}, new double[]{ca, cb}, new Edge[]{new Edge(0,1,0.5)});
    }

    @Test public void passiveExchangeConservesEnergyWithDifferentCapacities() {
        CellHeatSolver s = pair(50, 1);
        double before = s.totalEnergy();
        for (int i=0;i<1000;i++) s.step(0.5);
        assertEquals(before, s.totalEnergy(), 1e-8);
        assertEquals(before / 51, s.temperature(0), 1e-6);
        assertEquals(s.temperature(0), s.temperature(1), 1e-6);
    }

    @Test public void veryLightAirCannotOvershoot() {
        CellHeatSolver s = pair(50, .01);
        s.step(10);
        for (double t : s.temperatures()) assertTrue(t >= -40 && t <= 80);
    }

    @Test public void shuttingOffFirePreservesTemperature() {
        CellHeatSolver s = pair(50, 1);
        s.setPower(0, 20);
        s.step(5);
        double hot = s.temperature(0);
        s.setPower(0, 0);
        assertEquals(hot, s.temperature(0), 0);
        s.step(.5);
        assertTrue(s.temperature(0) > hot - 2);
    }

    @Test public void weatherChangesOnlyThroughBoundaryAndWall() {
        CellHeatSolver s = new CellHeatSolver(new double[]{20,20,20}, new double[]{1,50,1},
                new Edge[]{new Edge(0,1,.1),new Edge(1,2,.1)});
        s.setOutside(0, .5, -42);
        s.step(.1);
        assertTrue(s.temperature(0) < 20);
        assertEquals(20, s.temperature(1), 0);
        assertEquals(20, s.temperature(2), 0);
        s.step(.1);
        assertTrue(s.temperature(1) < 20);
        assertEquals(20, s.temperature(2), 0);
        s.step(.1);
        assertTrue(s.temperature(2) < 20);
    }

    @Test public void unloadedCellAndItsEdgesArePaused() {
        CellHeatSolver s = pair(50, 1);
        s.setLoaded(0, false);
        s.setPower(0, 20);
        s.setOutside(0, 1, -42);
        s.step(100);
        assertArrayEquals(new double[]{80,-40}, s.temperatures(), 0);
        s.setLoaded(0, true);
        s.setPower(0, 0);
        s.setOutside(0, 0, -42);
        s.step(.1);
        assertTrue(s.temperature(0) < 80 && s.temperature(0) > 79);
    }

    @Test public void replacementDoesNotResetNeighbours() {
        CellHeatSolver s = pair(50,1);
        s.replace(0,-10,20);
        assertEquals(-10,s.temperature(0),0);
        assertEquals(-40,s.temperature(1),0);
    }

    @Test public void sourceEnergyIsExactlyPowerTimesLoadedTime() {
        CellHeatSolver s = pair(50,1);
        double before = s.totalEnergy();
        s.setPower(0,20);
        s.step(5);
        assertEquals(100,s.totalEnergy()-before,1e-8);
    }

    @Test public void higherCapacitySlowsCooling() {
        CellHeatSolver heavy = pair(100,1), light = pair(10,1);
        heavy.step(.5); light.step(.5);
        assertTrue(heavy.temperature(0) > light.temperature(0));
    }

    @Test public void edgeOrderDoesNotChangeResult() {
        Edge[] forward = {new Edge(0,1,.5),new Edge(1,2,.2)};
        Edge[] reversed = {new Edge(2,1,.2),new Edge(1,0,.5)};
        CellHeatSolver a = new CellHeatSolver(new double[]{80,0,-40},new double[]{50,1,20},forward);
        CellHeatSolver b = new CellHeatSolver(new double[]{80,0,-40},new double[]{50,1,20},reversed);
        a.step(5); b.step(5);
        assertArrayEquals(a.temperatures(),b.temperatures(),1e-10);
    }

    @Test(expected=IllegalArgumentException.class) public void duplicateEdgesAreRejected() {
        new CellHeatSolver(new double[]{0,0},new double[]{1,1},new Edge[]{new Edge(0,1,1),new Edge(1,0,1)});
    }
    @Test(expected=IllegalArgumentException.class) public void zeroCapacityIsRejected() {
        pair(0,1);
    }
    @Test public void unsafeStepFailsBeforeChangingState() {
        CellHeatSolver s = pair(.001,.001);
        try { s.step(1000000); fail("expected excessive-step rejection"); }
        catch (IllegalArgumentException expected) { assertArrayEquals(new double[]{80,-40},s.temperatures(),0); }
    }
}
