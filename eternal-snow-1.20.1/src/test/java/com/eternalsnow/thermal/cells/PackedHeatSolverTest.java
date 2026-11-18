package com.eternalsnow.thermal.cells;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PackedHeatSolverTest {
    @Test public void matchesPreviousSolverWithSourcesBoundariesAndTopologyChanges() {
        int n=300;Random r=new Random(42);
        double[] t=new double[n],c=new double[n];
        for(int i=0;i<n;i++){t[i]=r.nextDouble()*100-40;c[i]=.5+r.nextDouble()*20;}
        var a=new CellHeatSolver(t,c,new CellHeatSolver.Edge[0]);
        var b=new ReferenceHeatSolver2103(t,c,new ReferenceHeatSolver2103.Edge[0]);
        for(int i=0;i<n-1;i++){a.setConnection(i,i+1,3);b.setConnection(i,i+1,3);}
        for(int tick=0;tick<120;tick++) {
            int i=r.nextInt(n-1);boolean loaded=tick%3!=0;
            a.setLoaded(i,loaded);b.setLoaded(i,loaded);
            double power=r.nextDouble()*60,boundary=r.nextDouble(),outside=r.nextDouble()*40-40;
            a.setPower(i,power);b.setPower(i,power);
            a.setOutside(i,boundary,outside);b.setOutside(i,boundary,outside);
            a.setConnection(i,i+1,boundary*3);b.setConnection(i,i+1,boundary*3);
            if(tick%17==0){a.replace(i,-10,2);b.replace(i,-10,2);}
            if(tick%19==0){a.setCapacity(i,3);b.setCapacity(i,3);}
            if(tick==50) {
                a.addCell(-30,1);b.addCell(-30,1);
                a.setConnection(n-1,n,2);b.setConnection(n-1,n,2);
            }
            a.step(.5);b.step(.5);
            assertArrayEquals("step "+tick,b.temperatures(),a.temperatures(),1e-8);
        }
    }
    @Test public void repeatedBoundaryChangesDoNotAccumulateCachedRates() {
        var s=new CellHeatSolver(new double[]{0,0},new double[]{1,1},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,3)});
        for(int i=0;i<100;i++) {s.setOutside(0,1,0);s.step(.5);assertEquals(3,s.lastSubsteps());}
        s.setLoaded(1,false);s.step(.5);assertEquals(1,s.lastSubsteps());
        s.setLoaded(1,true);s.step(.5);assertEquals(3,s.lastSubsteps());
    }
    @Test(timeout=30000) public void benchmarkMixedAirGridAtConvection100Percent() {
        int side=40,n=side*side*side;
        double[] t=new double[n],c=new double[n];Arrays.fill(c,1);Arrays.fill(t,-20);
        List<CellHeatSolver.Edge> edges=new ArrayList<>();
        for(int z=0;z<side;z++)for(int y=0;y<side;y++)for(int x=0;x<side;x++) {
            int i=x+side*(y+side*z);
            if(x+1<side)edges.add(new CellHeatSolver.Edge(i,i+1,3));
            if(y+1<side)edges.add(new CellHeatSolver.Edge(i,i+side,3));
            if(z+1<side)edges.add(new CellHeatSolver.Edge(i,i+side*side,3));
        }
        var a=new CellHeatSolver(t,c,edges.toArray(CellHeatSolver.Edge[]::new));
        var oldEdges=edges.stream().map(e->new ReferenceHeatSolver2103.Edge(e.a(),e.b(),e.conductance())).toArray(ReferenceHeatSolver2103.Edge[]::new);
        var b=new ReferenceHeatSolver2103(t,c,oldEdges);
        a.setPower(n/2,68);b.setPower(n/2,68);
        for(int i=0;i<5;i++){a.step(.5);b.step(.5);}
        long start=System.nanoTime();for(int i=0;i<30;i++)b.step(.5);long old=System.nanoTime()-start;
        start=System.nanoTime();for(int i=0;i<30;i++)a.step(.5);long packed=System.nanoTime()-start;
        assertArrayEquals(b.temperatures(),a.temperatures(),1e-8);
        assertEquals(10,a.lastSubsteps());
        System.out.println("THERMAL BENCHMARK 64000 cells, G=3, 30 steps: old="+old/1e6+"ms packed="+packed/1e6+"ms; synthetic, not server TPS");
    }
}
