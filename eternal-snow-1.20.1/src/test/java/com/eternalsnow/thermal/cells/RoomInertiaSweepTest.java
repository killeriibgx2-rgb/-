package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Calibration of a small wooden room and of a big brick room at once.
 *
 * <p>Air was modelled as conducting heat as well as brick (k = 1), so the shell of a room leaked
 * to the outdoor air at hundreds of W/K and a campfire could only lift the room by a degree.
 * Air is a poor conductor; the sweep below uses a realistic value and picks the source power
 * that satisfies both a small plank house and a big brick room.
 */
public class RoomInertiaSweepTest {
    private static final double OUTSIDE=-12;
    private static final double AIR_K=0.1;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** Small house: 5x5x3 interior, planks, open air outside, ground below. */
    static double WALL_K=2.5;
    /** A second room: 24x8x24 brick hall, for cross-checking against the big room tests. */
    static AdaptiveThermalTile house(double ts){
        System.err.println("CALCHECK house ts="+ts);
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=WorldCellThermal.AIR_HEAT_CAPACITY*ts;k[i]=AIR_K;g[i]=1;}
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            int i=AdaptiveThermalTile.index(x,y,z);g[i]=0;k[i]=AIR_K;
        }
        // Inner air must NOT be coupled to the weather directly; the whole point of a wall.
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)g[AdaptiveThermalTile.index(x,y,z)]=0;
        for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,0,z);air[i]=false;cap[i]=8*20*ts;k[i]=8;g[i]=1;
        }
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,air,0,y,z,pg,ts);plank(cap,k,g,air,6,y,z,pg,ts);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,y,0,pg,ts);plank(cap,k,g,air,x,y,6,pg,ts);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,0,z,pg,ts);plank(cap,k,g,air,x,4,z,pg,ts);}
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,boolean[] air,
                              int x,int y,int z,double pg,double ts){
        int i=AdaptiveThermalTile.index(x,y,z);
        air[i]=false;cap[i]=2.5*20*ts;k[i]=WALL_K;g[i]=pg;
    }
    /** outsideG as the world adapter sets it: 1 for air with sky, 1 for planks, 0 inside. */
    private static double g0(AdaptiveThermalTile t,int i){
        int x=i&7,y=i>>6,z=(i>>3)&7;
        boolean inside=x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3;
        return inside?0:1;
    }
    private static double airMean(AdaptiveThermalTile t,int x0,int x1,int y0,int y1,int z0,int z1){
        double s=0;int n=0;
        for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){
            s+=t.temperatures.get(AdaptiveThermalTile.index(x,y,z));n++;
        }
        return s/n;
    }

    static void curve(String label,AdaptiveThermalTile t,int[] box,double power){
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int cell=AdaptiveThermalTile.index(3,1,3);
        System.err.println("CALCHECK g(3,2,3)="+t.outsideG[AdaptiveThermalTile.index(3,2,3)]
            +" k(0,2,3)="+t.conductivities[AdaptiveThermalTile.index(0,2,3)]
            +" capAir="+t.capacities[AdaptiveThermalTile.index(3,2,3)]);
        double[] warm=new double[5];
        int[] marks={60,180,300,600,900};
        int s=0;
        for(int m=0;m<marks.length;m++){
            while(s<marks[m]*120){g.clearPower();g.addPower(t,cell,power);g.step(.5,OUTSIDE);s++;}
            warm[m]=airMean(t,box[0],box[1],box[2],box[3],box[4],box[5]);
        }
        double[] cool=new double[4];
        int[] after={10,60,300,900};
        int prev=0;
        for(int m=0;m<after.length;m++){
            while(prev<after[m]*120){g.clearPower();g.step(.5,OUTSIDE);prev++;}
            cool[m]=airMean(t,box[0],box[1],box[2],box[3],box[4],box[5]);
        }
        System.out.println(String.format(Locale.ROOT,
            "CAL %-26s 1m=%6.1f 3m=%6.1f 5m=%6.1f 10m=%6.1f 15m=%6.1f | off10s=%6.1f 1m=%6.1f 5m=%6.1f 15m=%6.1f",
            label,warm[0],warm[1],warm[2],warm[3],warm[4],cool[0],cool[1],cool[2],cool[3]));
    }

    @Test public void sweep(){
        int[] room={1,5,1,3,1,5};
        WALL_K=0.08;
        curve("ts30 P680",house(30),room,680);
        curve("ts30 P1500",house(30),room,1500);
        curve("ts30 P3000",house(30),room,3000);
        WALL_K=0.024;
        curve("ts30 P1500 wall.024",house(30),room,1500);
    }
}
