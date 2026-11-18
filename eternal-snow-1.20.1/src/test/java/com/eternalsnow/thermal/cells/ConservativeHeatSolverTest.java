package com.eternalsnow.thermal.cells;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ConservativeHeatSolverTest {
    @Test public void pairMatchesAnalyticSolutionAtExtremeConductivity() {
        var s=new CellHeatSolver(new double[]{80,-40},new double[]{50,.01},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,1000)});
        double energy=s.totalEnergy();s.stepConservative(.5);
        assertEquals(energy,s.totalEnergy(),1e-8);
        assertEquals(energy/50.01,s.temperature(0),1e-8);
        assertEquals(s.temperature(0),s.temperature(1),1e-8);
    }
    @Test public void weatherAndPowerMatchSingleCellAnalyticSolution() {
        var s=new CellHeatSolver(new double[]{20},new double[]{4},new CellHeatSolver.Edge[0]);
        s.setOutside(0,2,-20);s.setPower(0,10);
        s.stepConservative(.5);
        assertEquals(-15+(20+15)*Math.exp(-2*.5/4),s.temperature(0),1e-10);
        s.setOutside(0,0,0);double before=s.temperature(0);s.stepConservative(.5);
        assertEquals(before+10*.5/4,s.temperature(0),1e-10);
    }
    @Test public void unloadedNeighbourIsIsolatedAndNewEdgesAreInitialized() {
        var s=new CellHeatSolver(new double[]{80,-40},new double[]{50,1},new CellHeatSolver.Edge[0]);
        s.stepConservative(.5);s.setConnection(0,1,3);s.setLoaded(1,false);
        s.stepConservative(.5);assertArrayEquals(new double[]{80,-40},s.temperatures(),0);
        s.setLoaded(1,true);s.setCapacity(0,20);s.addCell(-30,1);s.setConnection(1,2,3);
        double e=s.totalEnergy();s.stepConservative(.5);assertEquals(e,s.totalEnergy(),1e-8);
        assertTrue(s.temperature(2)>-40);assertTrue(s.temperature(0)<80);
    }
    @Test public void splittingConvergesToFineReferenceWithoutRebasing() {
        var edges=new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,3),new CellHeatSolver.Edge(1,2,3),new CellHeatSolver.Edge(2,3,.2)};
        double[] initial={50,-20,-20,-20},cap={10,1,1,50};
        var reference=new CellHeatSolver(initial,cap,edges);
        var coarse=new CellHeatSolver(initial,cap,edges);
        var fine=new CellHeatSolver(initial,cap,edges);
        reference.setPower(0,10);coarse.setPower(0,10);fine.setPower(0,10);
        for(int i=0;i<10000;i++)reference.step(.001);
        for(int i=0;i<20;i++)coarse.stepConservative(.5);
        for(int i=0;i<1000;i++)fine.stepConservative(.01);
        double coarseError=0,fineError=0;
        for(int i=0;i<4;i++) {
            coarseError+=Math.abs(coarse.temperature(i)-reference.temperature(i));
            fineError+=Math.abs(fine.temperature(i)-reference.temperature(i));
        }
        assertTrue(fineError<coarseError);assertTrue(coarseError<1);
        assertEquals(reference.totalEnergy(),coarse.totalEnergy(),1e-7);
    }
    @Test public void spatialFrontierDoesNotFollowUniformWeatherShift() {
        assertFalse(ThermalFrontier.shouldExpand(-20,-35,-20,false));
        assertFalse(ThermalFrontier.shouldExpand(-35,-20,-35,false));
        assertTrue(ThermalFrontier.shouldExpand(5,-20,12,false));
        assertTrue(ThermalFrontier.shouldExpand(-30,-10,-36,false));
        assertTrue(ThermalFrontier.shouldExpand(5,-20,5,true));
        assertFalse(ThermalFrontier.shouldExpand(-20,-20.1,30,true));
    }
    @Test(timeout=30000) public void millionCellGraphPreservesEnergyWithFourSweeps() {
        int n=1000000;double[] t=new double[n],c=new double[n];Arrays.fill(t,-20);Arrays.fill(c,1);
        var edges=new CellHeatSolver.Edge[n-1];
        for(int i=0;i<n-1;i++)edges[i]=new CellHeatSolver.Edge(i,i+1,3);
        var s=new CellHeatSolver(t,c,edges);s.setPower(n/2,68);
        double energy=s.totalEnergy();long start=System.nanoTime();
        for(int i=0;i<10;i++)s.stepConservative(.5);
        assertEquals(energy+340,s.totalEnergy(),1e-5);assertEquals(2,s.lastSubsteps());
        System.out.println("Conservative million-cell test 10 steps ms="+(System.nanoTime()-start)/1e6);
    }
    @Test(timeout=30000) public void threeDimensionalMixingGridConservesInjectedPower() {
        int side=64,n=side*side*side;
        double[] t=new double[n],c=new double[n];Arrays.fill(t,-20);Arrays.fill(c,1);
        var s=new CellHeatSolver(t,c,new CellHeatSolver.Edge[0]);
        for(int z=0;z<side;z++)for(int y=0;y<side;y++)for(int x=0;x<side;x++) {
            int a=x+side*(y+side*z);
            if(x+1<side)s.setConnection(a,a+1,3);
            if(y+1<side)s.setConnection(a,a+side,3);
            if(z+1<side)s.setConnection(a,a+side*side,3);
        }
        s.setPower(n/2,68);double initial=s.totalEnergy();
        long start=System.nanoTime();
        for(int step=0;step<20;step++)s.stepConservative(.5);
        assertEquals(initial+680,s.totalEnergy(),1e-5);
        assertEquals(774144,s.edgeCount());
        System.out.println("Conservative 64^3 G=3 20 steps ms="+(System.nanoTime()-start)/1e6);
    }
}
