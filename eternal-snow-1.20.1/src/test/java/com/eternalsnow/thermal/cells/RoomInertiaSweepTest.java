package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Prints the cooling curve of a 5x5x3 plank room; asserts nothing. */
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
        int wall=AdaptiveThermalTile.index(3,5,3), ceiling=AdaptiveThermalTile.index(3,4,3);
        // 10 minutes of fire
        for(int s=0;s<1200;s++){g.clearPower();g.addPower(t,cell,power);g.step(.5,OUTSIDE);}
        double a0=air(t),w0=t.temperatures.get(ceiling);
        double[] steps=new double[6];
        int[] marks={20,120,600,1200,2400,6000};   // 10 s, 1 min, 5, 10, 20, 50 min
        int prev=0;
        for(int m=0;m<marks.length;m++){
            while(prev<marks[m]){g.clearPower();g.step(.5,OUTSIDE);prev++;}
            steps[m]=air(t);
        }
        System.out.println(String.format(Locale.ROOT,
            "COOL %-18s after10m: air=%6.1f ceil=%6.1f | 10s=%6.1f 1m=%6.1f 5m=%6.1f 10m=%6.1f 20m=%6.1f 50m=%6.1f (ceil now %.1f)",
            label,a0,w0,steps[0],steps[1],steps[2],steps[3],steps[4],steps[5],t.temperatures.get(ceiling)));
    }

    @Test public void sweep(){
        curve("ts=60 P=680",60,680);
        curve("ts=60 P=1500",60,1500);
        curve("ts=20 P=1500",20,1500);
        curve("ts=20 P=680",20,680);
    }
}
