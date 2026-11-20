package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Picking the source power once the time scale is real. Prints, never asserts. */
public class RoomInertiaSweepTest {
    private static final double OUTSIDE=-12;
    private static final double TS=WorldCellThermal.THERMAL_TIME_SCALE;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    static AdaptiveThermalTile room(double airCap,boolean hole){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3){air[i]=true;cap[i]=airCap;g[i]=0;}
        }
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,0,y,z,pg);plank(cap,k,g,6,y,z,pg);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,x,y,0,pg);plank(cap,k,g,x,y,6,pg);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){plank(cap,k,g,x,0,z,pg);plank(cap,k,g,x,4,z,pg);}
        double sg=WorldCellThermal.weatherCoupling(false,false,32,64,8);
        for(int i=0;i<512;i++){if(cap[i]!=1||air[i])continue;cap[i]=8*20*TS;k[i]=8;g[i]=sg;}
        if(hole){int i=AdaptiveThermalTile.index(3,4,3);air[i]=true;cap[i]=airCap;k[i]=1;g[i]=1;}
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,int x,int y,int z,double pg){
        int i=AdaptiveThermalTile.index(x,y,z);cap[i]=2.5*20*TS;k[i]=2.5;g[i]=pg;
    }
    private static double air(AdaptiveThermalTile t){
        double s=0;int n=0;for(int i=0;i<512;i++)if(t.air[i]){s+=t.temperatures.get(i);n++;}return s/n;
    }
    private static AdaptiveThermalTile run(double power,double minutes,boolean hole,double[] track){
        var t=room(WorldCellThermal.AIR_HEAT_CAPACITY*TS,hole);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int cell=AdaptiveThermalTile.index(3,1,3);
        int steps=(int)(minutes*120);
        for(int s=0;s<steps;s++){
            g.clearPower();if(power>0)g.addPower(t,cell,power);g.step(.5,OUTSIDE);
            if(track!=null&&s%(int)(120)==0)track[s/120]=air(t);
        }
        return t;
    }
    /** @return {1,2,5,10,20 min with fire, then +0.3,5,20,60 min after} */
    static double[] curve(double power){
        var t=room(WorldCellThermal.AIR_HEAT_CAPACITY*TS,false);
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int cell=AdaptiveThermalTile.index(3,1,3);
        double[] out=new double[9];
        int[] marks={120,240,600,1200,2400};
        int s=0;
        for(int m=0;m<marks.length;m++){
            while(s<marks[m]*120){g.clearPower();g.addPower(t,cell,power);g.step(.5,OUTSIDE);s++;}
            out[m]=air(t);
        }
        int[] after={36,600,2400,7200};
        int prev=0;
        for(int m=0;m<after.length;m++){
            while(prev<after[m]*120){g.clearPower();g.step(.5,OUTSIDE);prev++;}
            out[5+m]=air(t);
        }
        return out;
    }
    @Test public void sweep(){
        for(double p:new double[]{680,300,200,150,100,70}){
            double[] c=curve(p);
            System.out.println(String.format(Locale.ROOT,
                "PWR P=%-4.0f 1m=%6.1f 2m=%6.1f 5m=%6.1f 10m=%6.1f 20m=%6.1f | off 18s=%6.1f 5m=%6.1f 20m=%6.1f 60m=%6.1f",
                p,c[0],c[1],c[2],c[3],c[4],c[5],c[6],c[7],c[8]));
        }
    }
}
