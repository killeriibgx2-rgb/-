package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Prints warm/cool curves for a 5x5x3 plank room; asserts nothing. */
public class RoomInertiaSweepTest {
    private static final double OUTSIDE=-12;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    static AdaptiveThermalTile room(double ts){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3){air[i]=true;cap[i]=WorldCellThermal.AIR_HEAT_CAPACITY*ts;}
        }
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,0,y,z,pg,ts);plank(cap,k,g,6,y,z,pg,ts);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,x,y,0,pg,ts);plank(cap,k,g,x,y,6,pg,ts);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){plank(cap,k,g,x,0,z,pg,ts);plank(cap,k,g,x,4,z,pg,ts);}
        double sg=WorldCellThermal.weatherCoupling(false,false,32,64,8);
        for(int i=0;i<512;i++){if(cap[i]!=1||air[i])continue;cap[i]=8*20*ts;k[i]=8;g[i]=sg;}
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,int x,int y,int z,double pg,double ts){
        int i=AdaptiveThermalTile.index(x,y,z);cap[i]=2.5*20*ts;k[i]=2.5;g[i]=pg;
    }
    private static double air(AdaptiveThermalTile t){
        double s=0;int n=0;for(int i=0;i<512;i++)if(t.air[i]){s+=t.temperatures.get(i);n++;}return s/n;
    }

    static void curve(String label,double ts,double power){
        var t=room(ts);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int cell=AdaptiveThermalTile.index(3,1,3);
        int[] marks={60,180,300,600,900};   // seconds*? -> minutes 1,3,5,10,15
        double[] warm=new double[marks.length];
        int s=0;
        for(int m=0;m<marks.length;m++){
            int target=marks[m]*120;
            while(s<target){g.clearPower();g.addPower(t,cell,power);g.step(.5,OUTSIDE);s++;}
            warm[m]=air(t);
        }
        double[] cool=new double[4];
        int[] after={60,600,1200,1800};     // 1 s? -> seconds: 60*0.5=30 s, 5 min, 10 min, 15 min
        int prev=0;
        for(int m=0;m<after.length;m++){
            int target=after[m]*120;
            while(prev<target){g.clearPower();g.step(.5,OUTSIDE);prev++;}
            cool[m]=air(t);
        }
        int wallIdx=AdaptiveThermalTile.index(3,4,3), soilIdx=AdaptiveThermalTile.index(7,7,7);
        System.out.println(String.format(Locale.ROOT,
            "CURVE %-22s 1m=%6.1f 3m=%6.1f 5m=%6.1f 10m=%6.1f 15m=%6.1f | off30s=%6.1f off5m=%6.1f off10m=%6.1f off15m=%6.1f"
            +" || now: airC=%.1f wallC=%.0f soilC=%.0f wallT=%.1f soilT=%.1f",
            label,warm[0],warm[1],warm[2],warm[3],warm[4],cool[0],cool[1],cool[2],cool[3],
            t.capacities[AdaptiveThermalTile.index(3,1,3)],t.capacities[wallIdx],t.capacities[soilIdx],
            t.temperatures.get(wallIdx),t.temperatures.get(soilIdx)));
    }

    @Test public void sweep(){
        curve("ts=60 P=680",60,680);
        curve("ts=60 P=400",60,400);
    }
}
