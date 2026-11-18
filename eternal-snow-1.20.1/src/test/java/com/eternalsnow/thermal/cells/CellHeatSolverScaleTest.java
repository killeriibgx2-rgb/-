package com.eternalsnow.thermal.cells;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Regression for the 2.10.0 dedicated-server watchdog at HashMap$TreeNode.split. */
public class CellHeatSolverScaleTest {
    @Test(timeout=10000) public void manyIdenticalLegacyHashesStayBounded() {
        int n=262144;
        double[] temperatures=new double[n],capacity=new double[n];
        Arrays.fill(capacity,1);
        var edges=new CellHeatSolver.Edge[n/2];
        for(int i=0;i<edges.length;i++) {
            int a=2*i,b=a+1;
            assertEquals(1,Long.hashCode(((long)a<<32)|b));
            edges[edges.length-1-i]=new CellHeatSolver.Edge(a,b,.5);
            temperatures[a]=10;
        }
        var solver=new CellHeatSolver(temperatures,capacity,edges);
        solver.step(.5);
        assertEquals(7.5,solver.temperature(0),0);
        assertEquals(2.5,solver.temperature(1),0);
        assertEquals(10.0*edges.length,solver.totalEnergy(),1e-6);
    }

    @Test(timeout=10000) public void repeatedLargeGridRebuildsPreserveTemperaturesAndEnergy() {
        int side=64,n=side*side*side;
        double[] temperatures=new double[n],capacity=new double[n];
        Arrays.fill(temperatures,-20);Arrays.fill(capacity,1);
        var edges=new CellHeatSolver.Edge[3*(side-1)*side*side];
        int e=0;
        for(int z=0;z<side;z++)for(int y=0;y<side;y++)for(int x=0;x<side;x++) {
            int a=x+side*(y+side*z);
            if(x+1<side)edges[e++]=new CellHeatSolver.Edge(a,a+1,1);
            if(y+1<side)edges[e++]=new CellHeatSolver.Edge(a,a+side,1);
            if(z+1<side)edges[e++]=new CellHeatSolver.Edge(a,a+side*side,1);
        }
        assertEquals(edges.length,e);
        long start=System.nanoTime();
        for(int rebuild=0;rebuild<3;rebuild++) {
            var solver=new CellHeatSolver(temperatures,capacity,edges);
            solver.step(.5);
            assertEquals(-20.0*n,solver.totalEnergy(),0);
            temperatures=solver.temperatures();
        }
        System.out.println("Thermal regression: 3 x "+n+" cells / "+e+" edges + steps: "
                +(System.nanoTime()-start)/1_000_000+" ms (CI, not Minecraft TPS)");
    }

    @Test(expected=IllegalArgumentException.class) public void reversedDuplicateInLargeInputStillRejected() {
        int n=10000;
        var edges=new CellHeatSolver.Edge[n];
        double[] capacity=new double[n];Arrays.fill(capacity,1);
        for(int i=0;i<n-1;i++)edges[i]=new CellHeatSolver.Edge(i,i+1,1);
        edges[n-1]=new CellHeatSolver.Edge(n-1,n-2,2);
        new CellHeatSolver(new double[n],capacity,edges);
    }
}
