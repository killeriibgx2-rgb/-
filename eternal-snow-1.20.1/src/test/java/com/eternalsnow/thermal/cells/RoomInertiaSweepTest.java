package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Why a room has no inertia: measured time constants of the shipped parameters.
 *
 * <p>A player reports that a 5x5x3 plank room at -12 C warms slowly, and that the instant the
 * campfire dies the room is outdoor temperature again. That is not a bug in a formula: the
 * ratio capacity/leak of a plank shell is a few seconds, so the room is always at its
 * quasi-static equilibrium and holds nothing.
 *
 * <p>This class sweeps the two constants that decide it and prints the resulting warm/cool
 * curve, so the values are chosen from measurements rather than guesses.
 */
public class RoomInertiaSweepTest {

    private static final double OUTSIDE = -12;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** 5x5x3 interior, plank shell, surrounded by a shell of ground inside the same tile. */
    private static AdaptiveThermalTile room(double airCap,double blockMul,double surfaceScale,
                                            double groundScale){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            boolean inside=x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3;
            if(inside){air[i]=true;cap[i]=airCap;}
        }
        // Plank shell: six faces of the 5x5x3 box, one block thick.
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){
            plank(tile,cap,k,g,0,y,z,blockMul,surfaceScale);
            plank(tile,cap,k,g,6,y,z,blockMul,surfaceScale);
        }
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){
            plank(tile,cap,k,g,x,y,0,blockMul,surfaceScale);
            plank(tile,cap,k,g,x,y,6,blockMul,surfaceScale);
        }
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(tile,cap,k,g,x,0,z,blockMul,surfaceScale);        // floor
            plank(tile,cap,k,g,x,4,z,blockMul,surfaceScale);        // ceiling: sees the sky
        }
        // The rest of the tile is the ground around the house: a reservoir, not a heat store.
        for(int i=0;i<512;i++){
            if(cap[i]!=1||air[i])continue;
            cap[i]=8*blockMul;k[i]=8;g[i]=groundScale;
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

    /** @return {T at 2 min, 5 min, 15 min of fire, T 10 s after it dies, T 30 min after} */
    static double[] curve(double airCap,double blockMul,double surfaceScale,double groundScale,
                          double power){
        var tile=room(airCap,blockMul,surfaceScale,groundScale);
        var graph=new AdaptiveThermalGraph(List.of(tile));
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        double[] out=new double[5];
        for(int step=0;step<240;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[0]=air(tile);
        for(int step=0;step<360;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[1]=air(tile);
        for(int step=0;step<1200;step++){graph.clearPower();graph.addPower(tile,fire,power);graph.step(.5,OUTSIDE);}
        out[2]=air(tile);
        for(int step=0;step<20;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[3]=air(tile);
        for(int step=0;step<3580;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[4]=air(tile);
        return out;
    }

    private static void show(String label,double[] c){
        System.out.println(String.format(Locale.ROOT,
            "SWEEP %-34s 2min=%6.1f 5min=%6.1f 15min=%6.1f | +10s off=%6.1f | 30min off=%6.1f",
            label,c[0],c[1],c[2],c[3],c[4]));
    }

    @Test public void sweep(){
        double groundNow=1.0;
        show("shipped (air .02, mul 20, g 1.0)",curve(0.02,20,1.0,groundNow,680));
        show("g 0.05",curve(0.02,20,0.05,groundNow,680));
        show("g 0.02",curve(0.02,20,0.02,groundNow,680));
        show("air .25, g 0.02",curve(0.25,20,0.02,groundNow,680));
        show("air .25, g 0.02, mul 60",curve(0.25,60,0.02,groundNow,680));
        show("air .25, g 0.02, P 250",curve(0.25,20,0.02,groundNow,250));
        show("air .25, g 0.02, P 150",curve(0.25,20,0.02,groundNow,150));
        show("air .60, g 0.02, mul 60, P 150",curve(0.60,60,0.02,groundNow,150));
        show("air .25, g 0.08, P 680",curve(0.25,20,0.08,groundNow,680));
    }
}
