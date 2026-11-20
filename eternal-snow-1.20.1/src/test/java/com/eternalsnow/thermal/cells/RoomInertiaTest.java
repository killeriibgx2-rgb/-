package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: a 5x5x3 plank room at -12 C with one campfire in the middle warms too slowly,
 * a broken ceiling block shows outdoor temperature at once, and the moment the fire dies the
 * room is outdoor temperature again.
 *
 * <p>A house stands in open air: inside the plank shell nothing sees the sky, the ground is
 * below the floor, and the air outside is at the weather temperature.
 */
public class RoomInertiaTest {

    private static final double OUTSIDE=-12;
    private static final double CAMPFIRE=1500;      // heatCampfire 150 x roomSourcePower 10
    private static final double AIR=WorldCellThermal.AIR_HEAT_CAPACITY;
    private static final double TS=WorldCellThermal.THERMAL_TIME_SCALE;
    private static final double AIR_K=WorldCellThermal.AIR_CONDUCTIVITY;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    static AdaptiveThermalTile house(boolean hole){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;g[i]=1;}
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)
            g[AdaptiveThermalTile.index(x,y,z)]=0;                       // inner air sees no sky
        for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,0,z);
            air[i]=false;cap[i]=8*20*TS;k[i]=8;
            g[i]=WorldCellThermal.weatherCoupling(false,true,64,64,8);
        }
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,air,0,y,z,pg);plank(cap,k,g,air,6,y,z,pg);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,y,0,pg);plank(cap,k,g,air,x,y,6,pg);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(cap,k,g,air,x,0,z,pg);plank(cap,k,g,air,x,4,z,pg);
        }
        if(hole){
            int i=AdaptiveThermalTile.index(3,4,3);
            air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;g[i]=1;
        }
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,boolean[] air,
                              int x,int y,int z,double pg){
        int i=AdaptiveThermalTile.index(x,y,z);
        air[i]=false;cap[i]=2.5*20*TS;k[i]=2.5;g[i]=pg;
    }
    static double air(AdaptiveThermalTile t){
        double s=0;int n=0;
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            s+=t.temperatures.get(AdaptiveThermalTile.index(x,y,z));n++;
        }
        return s/n;
    }
    private static AdaptiveThermalGraph graph(AdaptiveThermalTile t){
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        return g;
    }
    private static int fireCell(){return AdaptiveThermalTile.index(3,1,3);}

    @Test public void theRoomWarmsInMinutesNotHours(){
        var t=house(false);var g=graph(t);
        for(int s=0;s<180;s++){g.clearPower();g.addPower(t,fireCell(),CAMPFIRE);g.step(.5,OUTSIDE);}
        double at90s=air(t);
        for(int s=0;s<1620;s++){g.clearPower();g.addPower(t,fireCell(),CAMPFIRE);g.step(.5,OUTSIDE);}
        double at15min=air(t);
        System.out.println(String.format(Locale.ROOT,
            "Plank room 5x5x3, one campfire at %.0f C outside: 1.5 min=%.1f C, 15 min=%.1f C",
            OUTSIDE,at90s,at15min));
        assertTrue("after a minute and a half the room must already be warmer, was "+at90s,
                at90s>OUTSIDE+3);
        assertTrue("and warm enough to live in after fifteen minutes, was "+at15min,
                at15min>5);
        assertTrue("but a campfire must not cook the room, was "+at15min,at15min<40);
    }

    @Test public void diagnoseWhereTheHeatGoes(){
        var t=house(false);var g=graph(t);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fireCell(),CAMPFIRE);g.step(.5,OUTSIDE);}
        double e0=energy(t),a0=air(t),w0=shell(t);
        for(int s=0;s<20;s++){g.clearPower();g.step(.5,OUTSIDE);}
        double e1=energy(t),a1=air(t),w1=shell(t);
        System.out.println(String.format(Locale.ROOT,
            "DIAG over 10 s: E %.0f -> %.0f (lost %.0f = %.0f W); air %.2f->%.2f C; shell %.2f->%.2f C",
            e0,e1,e0-e1,(e0-e1)/10,a0,a1,w0,w1));
        double cAir=0,cShell=0;
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)
            cAir+=t.capacities[AdaptiveThermalTile.index(x,y,z)];
        for(int y=0;y<=4;y++)for(int z=0;z<=6;z++)for(int x=0;x<=6;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(t.air!=null&&!t.air[i])cShell+=t.capacities[i];
        }
        System.out.println(String.format(Locale.ROOT,
            "DIAG capacities: air=%.1f shell=%.1f  =>  G_room_to_out approx %.1f W/K",
            cAir,cShell,(e0-e1)/10/Math.max(0.1,a0-OUTSIDE)));
    }

    private static double energy(AdaptiveThermalTile t){
        double e=0;
        for(int i=0;i<512;i++)e+=t.capacities[i]*t.temperatures.get(i);
        return e;
    }
    private static double shell(AdaptiveThermalTile t){
        double s=0;int n=0;
        for(int y=0;y<=4;y++)for(int z=0;z<=6;z++)for(int x=0;x<=6;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(t.air!=null&&!t.air[i]){s+=t.temperatures.get(i);n++;}
        }
        return n==0?0:s/n;
    }

    @Test public void theRoomHoldsHeatWhenTheFireDies(){
        var t=house(false);var g=graph(t);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fireCell(),CAMPFIRE);g.step(.5,OUTSIDE);}
        double warm=air(t);
        for(int s=0;s<20;s++){g.clearPower();g.step(.5,OUTSIDE);}
        double after10s=air(t);
        for(int s=0;s<280;s++){g.clearPower();g.step(.5,OUTSIDE);}
        double after2min=air(t);
        System.out.println(String.format(Locale.ROOT,
            "Fire out: %.1f C, 10 s later=%.1f C, 2 min later=%.1f C",warm,after10s,after2min));
        assertTrue("ten seconds after the fire dies the room must still be warm, was "+after10s,
                after10s>warm-3);
        assertTrue("two minutes later it must still hold heat, was "+after2min,
                after2min>OUTSIDE+3);
    }

    @Test public void oneHoleInTheCeilingDoesNotFreezeTheRoom(){
        var closed=house(false);var holed=house(true);
        var gc=graph(closed);var gh=graph(holed);
        for(int s=0;s<1800;s++){
            gc.clearPower();gc.addPower(closed,fireCell(),CAMPFIRE);gc.step(.5,OUTSIDE);
            gh.clearPower();gh.addPower(holed,fireCell(),CAMPFIRE);gh.step(.5,OUTSIDE);
        }
        for(int s=0;s<60;s++){gh.clearPower();gh.step(.5,OUTSIDE);}   // 30 s with the hole open
        double underHole=holed.temperatures.get(AdaptiveThermalTile.index(3,3,3));
        System.out.println(String.format(Locale.ROOT,
            "Hole open 30 s: block under it=%.1f C, room mean=%.1f C (closed room %.1f C)",
            underHole,air(holed),air(closed)));
        assertTrue("one missing ceiling block must not import the weather, was "+underHole,
                underHole>OUTSIDE+4);
    }
}
