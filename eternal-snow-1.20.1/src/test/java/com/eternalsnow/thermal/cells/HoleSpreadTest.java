package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: the room is +11 C, one ceiling block is broken in a corner, and "gradually it
 * becomes cold only under the hole, nothing freezes in the other part of the room". A hole must
 * pull the whole room towards the weather, because a room is one connected volume of air.
 */
public class HoleSpreadTest {

    private static final double OUTSIDE = -15;
    private static final double AIR = WorldCellThermal.AIR_HEAT_CAPACITY;
    private static final double TS = WorldCellThermal.THERMAL_TIME_SCALE;
    private static final double AIR_K = WorldCellThermal.AIR_CONDUCTIVITY;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    static AdaptiveThermalTile room(boolean hole){
        var t=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;
            g[i]=WorldCellThermal.AIR_OPEN_COUPLING;}
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)
            g[AdaptiveThermalTile.index(x,y,z)]=0;
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,air,0,y,z,pg);plank(cap,k,g,air,6,y,z,pg);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,y,0,pg);plank(cap,k,g,air,x,y,6,pg);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,0,z,pg);plank(cap,k,g,air,x,4,z,pg);}
        if(hole){
            int i=AdaptiveThermalTile.index(1,4,1);       // a corner ceiling block
            air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;g[i]=WorldCellThermal.AIR_OPEN_COUPLING;
        }
        WorldCellThermal.applyVents(air,g);
        t.configure(true,cap,k,g,filled(1),filled(0));
        t.fineMaterials(filled(1),air,3);
        return t;
    }
    private static void plank(double[] cap,double[] k,double[] g,boolean[] air,int x,int y,int z,double pg){
        int i=AdaptiveThermalTile.index(x,y,z);air[i]=false;cap[i]=2.5*20*TS;k[i]=2.5;g[i]=pg;
    }
    private static double air(AdaptiveThermalTile t){
        double s=0;int n=0;
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            s+=t.temperatures.get(AdaptiveThermalTile.index(x,y,z));n++;
        }
        return s/n;
    }

    private static double[] curve(boolean hole){
        var t=room(hole);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fire,1500);g.step(.5,OUTSIDE);}
        double[] out=new double[5];
        out[0]=air(t);
        int[] minutes={1,2,5,10};
        int prev=0;
        for(int m=0;m<minutes.length;m++){
            while(prev<minutes[m]*120){g.clearPower();g.step(.5,OUTSIDE);prev++;}
            out[m+1]=air(t);
        }
        return out;
    }

    @Test public void aHoleCoolsTheWholeRoomNotJustItsCorner(){
        double[] closed=curve(false),holed=curve(true);
        System.out.println(String.format(Locale.ROOT,
            "Closed room: %.1f C. Hole open: 0 min=%.1f, 1 min=%.1f, 2 min=%.1f, 5 min=%.1f, 10 min=%.1f",
            closed[0],holed[0],holed[1],holed[2],holed[3],holed[4]));
        assertTrue("the room must get colder with a hole, was "+holed[4]+" vs closed "+closed[0],
                holed[4]<closed[0]-3);
        assertTrue("but not instantly: 60 s after opening it must still be warm, was "+holed[1],
                holed[1]>OUTSIDE+5);
        assertTrue("and it must keep falling, "+holed[2]+" -> "+holed[4],holed[4]<holed[2]);
    }

    @Test public void theColdSpreadsFromTheHoleAcrossTheFloor(){
        var t=room(true);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fire,1500);g.step(.5,OUTSIDE);}
        for(int s=0;s<600;s++){g.clearPower();g.step(.5,OUTSIDE);}     // five minutes open
        double underHole=t.temperatures.get(AdaptiveThermalTile.index(1,1,1));
        double farCorner=t.temperatures.get(AdaptiveThermalTile.index(5,1,5));
        System.out.println(String.format(Locale.ROOT,
            "Five minutes with the hole open: under the hole=%.1f C, far corner=%.1f C",
            underHole,farCorner));
        assertTrue("the far corner must cool too, was "+farCorner,farCorner<underHole);
    }
}
