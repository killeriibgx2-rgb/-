package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * The owner's room is a house standing in the open, not a cave: outside its plank walls is AIR
 * at -12 C, and the ground is below the floor. The earlier sweep surrounded the room with rock,
 * which turned the walls into a short circuit to a cold infinite reservoir — wrong geometry,
 * wrong conclusion. This prints curves for the real geometry so the constants can be chosen.
 */
public class RoomInertiaSweepTest {
    private static final double OUTSIDE=-12;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** 5x5x3 interior, one block of planks around it, open air outside, ground at the bottom. */
    static AdaptiveThermalTile room(double ts){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=WorldCellThermal.AIR_HEAT_CAPACITY*ts;g[i]=1;}
        // ground layer
        for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,0,z);air[i]=false;cap[i]=8*20*ts;k[i]=8;
            g[i]=WorldCellThermal.weatherCoupling(false,true,64,64,8);
        }
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,air,0,y,z,pg,ts);plank(cap,k,g,air,6,y,z,pg,ts);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,y,0,pg,ts);plank(cap,k,g,air,x,y,6,pg,ts);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(cap,k,g,air,x,0,z,pg,ts);plank(cap,k,g,air,x,4,z,pg,ts);
        }
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,boolean[] air,
                              int x,int y,int z,double pg,double ts){
        int i=AdaptiveThermalTile.index(x,y,z);
        air[i]=false;cap[i]=2.5*20*ts;k[i]=2.5;g[i]=pg;
    }
    private static double air(AdaptiveThermalTile t){
        double s=0;int n=0;
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            s+=t.temperatures.get(AdaptiveThermalTile.index(x,y,z));n++;
        }
        return s/n;
    }

    static void curve(double ts,double power){
        var t=room(ts);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int cell=AdaptiveThermalTile.index(3,1,3);
        double[] warm=new double[5];
        int[] marks={60,180,300,600,900};
        int s=0;
        for(int m=0;m<marks.length;m++){
            while(s<marks[m]*120){g.clearPower();g.addPower(t,cell,power);g.step(.5,OUTSIDE);s++;}
            warm[m]=air(t);
        }
        double[] cool=new double[6];
        int[] after={20,120,600,1200,2400,6000};
        int prev=0;
        for(int m=0;m<after.length;m++){
            while(prev<after[m]*120){g.clearPower();g.step(.5,OUTSIDE);prev++;}
            cool[m]=air(t);
        }
        System.out.println(String.format(Locale.ROOT,
            "HOUSE ts=%-3.0f P=%-4.0f 1m=%6.1f 3m=%6.1f 5m=%6.1f 10m=%6.1f 15m=%6.1f | off10s=%6.1f 1m=%6.1f 5m=%6.1f 10m=%6.1f 20m=%6.1f 50m=%6.1f",
            ts,power,warm[0],warm[1],warm[2],warm[3],warm[4],cool[0],cool[1],cool[2],cool[3],cool[4],cool[5]));
    }

    @Test public void sweep(){
        curve(1,680); curve(1,300);
        curve(5,680); curve(5,300);
        curve(20,680);
    }
}
