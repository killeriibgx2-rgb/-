package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Choosing the constants for room inertia, from measurement.
 *
 * <p>Air capacity 0.02 with weather coupling 1.0 gives one cell a time constant of 0.02 s, so a
 * room is always exactly at its equilibrium: the fire goes out and the thermometer is at outdoor
 * temperature ten seconds later, as the owner reported. The same numbers make a hole in a
 * ceiling show outdoor temperature on the next block, because that block is coupled to the sky
 * directly and instantly.
 *
 * <p>Two independent knobs are measured here: how much heat a cell holds, and how strongly a
 * solid surface leaks to the weather. A wall block touches the weather on one face out of six, so
 * a coupling of 1.0 is a short circuit.
 */
public class RoomInertiaSweepTest {

    private static final double OUTSIDE = -12;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    static AdaptiveThermalTile room(double airCap,double airSkyCap,double blockMul,
                                    double surfaceScale,double soilScale,boolean hole){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3){air[i]=true;cap[i]=airCap;}
        }
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){
            plank(cap,k,g,0,y,z,blockMul,surfaceScale);
            plank(cap,k,g,6,y,z,blockMul,surfaceScale);
        }
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){
            plank(cap,k,g,x,y,0,blockMul,surfaceScale);
            plank(cap,k,g,x,y,6,blockMul,surfaceScale);
        }
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(cap,k,g,x,0,z,blockMul,surfaceScale);
            plank(cap,k,g,x,4,z,blockMul,surfaceScale);
        }
        for(int i=0;i<512;i++){
            if(cap[i]!=1||air[i])continue;
            cap[i]=8*blockMul;k[i]=8;g[i]=soilScale;
        }
        if(hole){int i=AdaptiveThermalTile.index(3,4,3);air[i]=true;cap[i]=airSkyCap;k[i]=1;g[i]=1;}
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,int x,int y,int z,
                              double blockMul,double surfaceScale){
        int i=AdaptiveThermalTile.index(x,y,z);
        cap[i]=2.5*blockMul;k[i]=2.5;g[i]=surfaceScale;
    }
    private static double air(AdaptiveThermalTile tile){
        double sum=0;int n=0;
        for(int i=0;i<512;i++)if(tile.air[i]){sum+=tile.temperatures.get(i);n++;}
        return sum/n;
    }

    /** @return {2m, 5m, 15m with fire, +10s after it dies, 5m, 30m, hole cell 30 s after opening} */
    static double[] curve(double airCap,double airSky,double blockMul,double surface,double soil,
                          double power){
        var tile=room(airCap,airSky,blockMul,surface,soil,false);
        var graph=new AdaptiveThermalGraph(List.of(tile));
        graph.setRoomAirMixing(20);graph.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        double[] out=new double[7];
        for(int step=0;step<240;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[0]=air(tile);
        for(int step=0;step<360;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[1]=air(tile);
        for(int step=0;step<1200;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[2]=air(tile);
        for(int step=0;step<20;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[3]=air(tile);
        for(int step=0;step<600;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[4]=air(tile);
        for(int step=0;step<3000;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[5]=air(tile);
        // Now open one ceiling block and watch the cell right under it.
        var holed=room(airCap,airSky,blockMul,surface,soil,true);
        var g2=new AdaptiveThermalGraph(List.of(holed));
        g2.setRoomAirMixing(20);g2.setMixingSweeps(12);
        int f2=AdaptiveThermalTile.index(3,1,3);
        for(int step=0;step<1800;step++){g2.clearPower();g2.addPower(holed,f2,power);g2.step(.5,OUTSIDE);}
        for(int step=0;step<60;step++){g2.clearPower();g2.step(.5,OUTSIDE);}   // 30 s after opening
        out[6]=holed.temperatures.get(AdaptiveThermalTile.index(3,3,3));
        return out;
    }

    private static void show(String label,double[] c){
        System.out.println(String.format(Locale.ROOT,
            "SWEEP %-30s 2m=%6.1f 5m=%6.1f 15m=%6.1f | +10s=%6.1f 5m=%6.1f 30m=%6.1f | under hole=%6.1f",
            label,c[0],c[1],c[2],c[3],c[4],c[5],c[6]));
    }

    @Test public void sweep(){
        show("shipped .02/1.0 mul20 g1 s1 P680",curve(0.02,0.02,20,1.0,1.0,680));
        show(".5/.5 mul20 g.08 s.08 P680",curve(0.5,0.5,20,0.08,0.08,680));
        show(".5/.5 mul20 g.05 s.05 P680",curve(0.5,0.5,20,0.05,0.05,680));
        show(".5/.5 mul20 g.05 s.05 P400",curve(0.5,0.5,20,0.05,0.05,400));
        show("1.0/1.0 mul20 g.05 s.05 P680",curve(1.0,1.0,20,0.05,0.05,680));
        show("1.0/1.0 mul20 g.05 s.05 P400",curve(1.0,1.0,20,0.05,0.05,400));
        show(".5/.5 mul60 g.05 s.05 P680",curve(0.5,0.5,60,0.05,0.05,680));
    }
}
