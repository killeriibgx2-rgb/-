package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Measured time constants of the shipped parameters.
 *
 * <p>Owner's report: a 5x5x3 plank room at -12 C warms slowly, and the instant the campfire goes
 * out it is outdoor temperature again. The reason is arithmetic: air capacity is 0.02 while its
 * weather coupling is 1.0 per cell, so the time constant of one cell is 0.02 s. The room is
 * always exactly at its quasi-static equilibrium and holds nothing.
 *
 * <p>This class sweeps the four constants that decide it and prints the resulting curve, so the
 * new values are chosen from measurements, not guesses.
 */
public class RoomInertiaSweepTest {

    private static final double OUTSIDE = -12;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /**
     * 5x5x3 interior with a plank shell, standing in the ground: the rest of the tile is soil
     * coupled to the weather at the surface.
     */
    private static AdaptiveThermalTile room(double airCap,double blockMul,double surfaceScale,
                                            double soilScale){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            boolean inside=x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3;
            if(inside){air[i]=true;cap[i]=airCap;}
        }
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){
            plank(tile,cap,k,g,0,y,z,blockMul,surfaceScale);
            plank(tile,cap,k,g,6,y,z,blockMul,surfaceScale);
        }
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){
            plank(tile,cap,k,g,x,y,0,blockMul,surfaceScale);
            plank(tile,cap,k,g,x,y,6,blockMul,surfaceScale);
        }
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(tile,cap,k,g,x,0,z,blockMul,surfaceScale);
            plank(tile,cap,k,g,x,4,z,blockMul,surfaceScale);
        }
        for(int i=0;i<512;i++){
            if(cap[i]!=1||air[i])continue;
            cap[i]=8*blockMul;k[i]=8;g[i]=soilScale;
        }
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(AdaptiveThermalTile t,double[] cap,double[] k,double[] g,
                              int x,int y,int z,double blockMul,double surfaceScale){
        int i=AdaptiveThermalTile.index(x,y,z);
        cap[i]=2.5*blockMul;k[i]=2.5;g[i]=surfaceScale;
    }
    private static double air(AdaptiveThermalTile tile){
        double sum=0;int n=0;
        for(int i=0;i<512;i++)if(tile.air[i]){sum+=tile.temperatures.get(i);n++;}
        return sum/n;
    }

    /** @return {T at 2, 5, 15 min of fire, T 10 s after it dies, T 10 min after, T 30 min after} */
    static double[] curve(double airCap,double blockMul,double surfaceScale,double soilScale,
                          double power){
        var tile=room(airCap,blockMul,surfaceScale,soilScale);
        var graph=new AdaptiveThermalGraph(List.of(tile));
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        double[] out=new double[6];
        for(int step=0;step<240;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[0]=air(tile);
        for(int step=0;step<360;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[1]=air(tile);
        for(int step=0;step<1200;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[2]=air(tile);
        for(int step=0;step<20;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[3]=air(tile);
        for(int step=0;step<1200;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[4]=air(tile);
        for(int step=0;step<2400;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[5]=air(tile);
        return out;
    }

    private static void show(String label,double[] c){
        System.out.println(String.format(Locale.ROOT,
            "SWEEP %-36s 2m=%6.1f 5m=%6.1f 15m=%6.1f | +10s=%6.1f 10m=%6.1f 30m=%6.1f",
            label,c[0],c[1],c[2],c[3],c[4],c[5]));
    }

    @Test public void sweep(){
        show("shipped air .02 mul20 g1 soil1 P680",curve(0.02,20,1.0,1.0,680));
        show("air .12 mul20 g1 soil1 P680",curve(0.12,20,1.0,1.0,680));
        show("air .12 mul20 g.1 soil.1 P680",curve(0.12,20,0.1,0.1,680));
        show("air .12 mul20 g.03 soil.03 P680",curve(0.12,20,0.03,0.03,680));
        show("air .12 mul20 g.1 soil.1 P150",curve(0.12,20,0.1,0.1,150));
        show("air .12 mul20 g.1 soil.1 P100",curve(0.12,20,0.1,0.1,100));
        show("air .12 mul20 g.03 soil.1 P150",curve(0.12,20,0.03,0.1,150));
        show("air .30 mul20 g.1 soil.1 P150",curve(0.30,20,0.1,0.1,150));
        show("air .12 mul8  g.1 soil.1 P150",curve(0.12,8,0.1,0.1,150));
        show("air .12 mul20 g.05 soil.05 P150",curve(0.12,20,0.05,0.05,150));
    }
}
