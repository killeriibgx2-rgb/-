package com.eternalsnow.thermal.cells;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.*;

public class AirHeatTransferTest {
    @Test public void mixingCannotBypassWallsAndCanBeDisabled() {
        assertEquals(.4,AirHeatTransfer.conductance(1,.25,true,false,true,100),1e-12);
        assertEquals(1,AirHeatTransfer.conductance(1,1,true,true,false,100),0);
        assertEquals(1,AirHeatTransfer.conductance(1,1,true,true,true,0),0);
        assertEquals(3,AirHeatTransfer.conductance(1,1,true,true,true,100),0);
    }
    @Test public void flameSplitConservesPowerWithAnyNumberOfAirFaces() {
        for(int neighbours=0;neighbours<=6;neighbours++) {
            double share=AirHeatTransfer.airShare(true,neighbours);
            double sum=68*(1-share);
            for(int i=0;i<neighbours;i++)sum+=68*share/neighbours;
            assertEquals(68,sum,1e-10);
        }
        assertEquals(0,AirHeatTransfer.airShare(false,6),0);
    }
    private static int index(int x,int y,int z){return x+11*(y+7*z);}
    private static boolean air(int x,int y,int z){return x>0&&x<10&&y>0&&y<6&&z>0&&z<6 && !(x==3&&y==1&&z==3);}
    private static CellHeatSolver room(boolean improved) {
        int n=11*7*7;double[] t=new double[n],c=new double[n],k=new double[n];boolean[] a=new boolean[n];
        Arrays.fill(t,-20);
        for(int z=0;z<7;z++)for(int y=0;y<7;y++)for(int x=0;x<11;x++) {
            int i=index(x,y,z);a[i]=air(x,y,z);c[i]=a[i]?1:50;k[i]=a[i]?1:.24;
        }
        int fire=index(3,1,3);c[fire]=160;k[fire]=.72;
        var edges=new ArrayList<CellHeatSolver.Edge>();
        for(int z=0;z<7;z++)for(int y=0;y<7;y++)for(int x=0;x<11;x++) {
            int i=index(x,y,z);
            int[] neighbours={x<10?index(x+1,y,z):-1,y<6?index(x,y+1,z):-1,z<6?index(x,y,z+1):-1};
            for(int j:neighbours)if(j>=0)edges.add(new CellHeatSolver.Edge(i,j,
                    AirHeatTransfer.conductance(k[i],k[j],a[i],a[j],improved,100)));
        }
        var solver=new CellHeatSolver(t,c,edges.toArray(CellHeatSolver.Edge[]::new));
        // Boundary at exterior faces only; no outdoor relaxation on interior air.
        for(int z=0;z<7;z++)for(int y=0;y<7;y++)for(int x=0;x<11;x++)
            if(x==0||x==10||y==0||y==6||z==0||z==6)solver.setOutside(index(x,y,z),.24,-20);
        int[] recipients={index(2,1,3),index(4,1,3),index(3,2,3),index(3,1,2),index(3,1,4)};
        double share=AirHeatTransfer.airShare(improved,recipients.length);
        solver.setPower(fire,68*(1-share));
        for(int i:recipients)solver.setPower(i,68*share/recipients.length);
        for(int step=0;step<1200;step++)solver.step(.5);
        return solver;
    }
    @Test(timeout=10000) public void enclosedRoomWarmsBeyondOneBlockWithoutIncreasingPower() {
        var old=room(false);var fixed=room(true);
        for(int distance=1;distance<=4;distance++) {
            double t=fixed.temperature(index(3+distance,1,3));
            System.out.println("Closed-room model, distance "+distance+": old="+old.temperature(index(3+distance,1,3))+", new="+t);
            assertTrue("air at distance "+distance+" must warm",t>-19);
        }
        double oldDrop=old.temperature(index(4,1,3))-old.temperature(index(7,1,3));
        double newDrop=fixed.temperature(index(4,1,3))-fixed.temperature(index(7,1,3));
        assertTrue("mixing reduces local hot spot",newDrop<oldDrop);
        assertTrue("upper tower air warms",fixed.temperature(index(3,5,3))>-19);
    }
}
