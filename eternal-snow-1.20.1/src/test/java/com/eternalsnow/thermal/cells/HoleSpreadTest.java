package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Does opening one ceiling block cool the WHOLE room, or only the spot under the hole? */
public class HoleSpreadTest {
    private static final double OUTSIDE=-15;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}
    private static final double AIR=WorldCellThermal.AIR_HEAT_CAPACITY;
    private static final double TS=WorldCellThermal.THERMAL_TIME_SCALE;
    private static final double AIR_K=WorldCellThermal.AIR_CONDUCTIVITY;

    static AdaptiveThermalTile room(boolean hole){
        var t=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;g[i]=1;}
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)g[AdaptiveThermalTile.index(x,y,z)]=0;
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,air,0,y,z,pg);plank(cap,k,g,air,6,y,z,pg);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,y,0,pg);plank(cap,k,g,air,x,y,6,pg);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,0,z,pg);plank(cap,k,g,air,x,4,z,pg);}
        if(hole){int i=AdaptiveThermalTile.index(1,4,1);air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;g[i]=1;}
        t.configure(true,cap,k,g,filled(1),filled(0));
        t.fineMaterials(filled(1),air,3);
        return t;
    }
    private static void plank(double[] cap,double[] k,double[] g,boolean[] air,int x,int y,int z,double pg){
        int i=AdaptiveThermalTile.index(x,y,z);air[i]=false;cap[i]=2.5*20*TS;k[i]=2.5;g[i]=pg;
    }
    private static String row(AdaptiveThermalTile t,int y){
        StringBuilder sb=new StringBuilder();
        for(int x=0;x<8;x++)sb.append(String.format(Locale.ROOT,"%6.1f",t.temperatures.get(AdaptiveThermalTile.index(x,y,1))));
        return sb.toString();
    }

    @Test public void holeCoolsWholeRoom(){
        var t=room(true);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fire,1500);g.step(.5,OUTSIDE);}
        for(int minute=1;minute<=10;minute++){
            for(int s=0;s<120;s++){g.clearPower();g.step(.5,OUTSIDE);}
            System.out.println("HOLE min="+minute+" y1: "+row(t,1));
        }
    }
}
