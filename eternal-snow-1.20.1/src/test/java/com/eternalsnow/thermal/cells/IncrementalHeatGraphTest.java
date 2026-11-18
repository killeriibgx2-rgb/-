package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class IncrementalHeatGraphTest {
    @Test public void growingGraphMatchesFullRebuildWithoutResettingOldTemperatures() {
        var incremental=new CellHeatSolver(new double[0],new double[0],new CellHeatSolver.Edge[0]);
        double[] previous=new double[0];
        for(int n=1;n<=100;n++) {
            double[] t=Arrays.copyOf(previous,n),c=new double[n];Arrays.fill(c,2);
            t[n-1]=-20;
            assertEquals(n-1,incremental.addCell(-20,2));
            var edges=new CellHeatSolver.Edge[n-1];
            for(int j=0;j<n-1;j++)edges[j]=new CellHeatSolver.Edge(j,j+1,.5);
            if(n>1)incremental.setConnection(n-2,n-1,.5);
            var rebuilt=new CellHeatSolver(t,c,edges);
            incremental.setPower(0,20);rebuilt.setPower(0,20);
            incremental.step(.5);rebuilt.step(.5);
            assertArrayEquals(rebuilt.temperatures(),incremental.temperatures(),1e-10);
            previous=rebuilt.temperatures();
        }
    }
    @Test public void repeatedReverseUpdatesDoNotDuplicateEnergyExchange() {
        var s=new CellHeatSolver(new double[]{80,-20},new double[]{10,1},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,.5)});
        for(int i=0;i<50;i++)s.setConnection(1,0,.25);
        var expected=new CellHeatSolver(new double[]{80,-20},new double[]{10,1},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,.25)});
        s.step(1);expected.step(1);
        assertArrayEquals(expected.temperatures(),s.temperatures(),1e-10);
    }
    @Test public void localReplacementAndPauseMatchRebuild() {
        var s=new CellHeatSolver(new double[]{80,-20},new double[]{10,1},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,.5)});
        s.addCell(-30,1);s.setConnection(1,2,.5);
        s.replace(1,-10,30);s.setConnection(0,1,.1);s.setConnection(1,2,.1);
        var expected=new CellHeatSolver(new double[]{80,-10,-30},new double[]{10,30,1},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,.1),new CellHeatSolver.Edge(1,2,.1)});
        s.setLoaded(2,false);expected.setLoaded(2,false);
        s.step(.5);expected.step(.5);
        assertArrayEquals(expected.temperatures(),s.temperatures(),1e-10);
        assertEquals(-30,s.temperature(2),0);
    }
    @Test(timeout=10000) public void largeGraphSupportsManyLocalEditsWithoutReconstruction() {
        int n=100000;
        var s=new CellHeatSolver(new double[0],new double[0],new CellHeatSolver.Edge[0]);
        for(int i=0;i<n;i++) {s.addCell(-20,1);if(i>0)s.setConnection(i-1,i,1);}
        for(int change=0;change<10000;change++) {
            int i=change*7%(n-1);s.setCapacity(i,2);s.setConnection(i,i+1,.4);
        }
        assertEquals(n,s.size());assertEquals(n,s.temperatures().length);
        s.step(.5);
        for(double value:s.temperatures())assertEquals(-20,value,0);
    }
}
