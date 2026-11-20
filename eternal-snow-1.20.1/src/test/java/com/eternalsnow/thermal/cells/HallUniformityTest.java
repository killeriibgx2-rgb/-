package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A wide hall must not show 8-block temperature steps at tile borders. This is the geometry
 * that matters for bunker rooms: many tiles side by side, one fire.
 */
public class HallUniformityTest {

    private static final double AIR_CAPACITY=0.02;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** A 4x1x4 tunnel of tiles: solid shell, air inside, sealed at both ends. */
    private static List<AdaptiveThermalTile> hall(){
        int sx=32,sy=8,sz=32;
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int ty=0;ty<1;ty++)for(int tz=0;tz<4;tz++)for(int tx=0;tx<4;tx++){
            var tile=new AdaptiveThermalTile(tx*8,ty*8,tz*8,new TileTemperatureState(filled(-20)));
            double[] cap=filled(1),k=filled(1),g=filled(0);
            boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=tx*8+x,gy=ty*8+y,gz=tz*8+z;
                boolean shell=gx==0||gy==0||gz==0||gx==sx-1||gy==sy-1||gz==sz-1;
                int i=AdaptiveThermalTile.index(x,y,z);
                if(shell){cap[i]=5.0*20;k[i]=0.42;g[i]=0.42;}
                else{air[i]=true;cap[i]=AIR_CAPACITY;}
            }
            tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            list.add(tile);
        }
        return list;
    }

    private static double[] run(double mixing){
        var tiles=hall();
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(mixing);
        var source=tiles.get(0);
        int cell=AdaptiveThermalTile.index(4,4,4);
        for(int step=0;step<2400;step++){
            graph.clearPower();
            graph.addPower(source,cell,68*10);
            graph.step(.5,-20);
        }
        // Sample the middle height across the hall.
        double[] line=new double[32];
        for(int x=0;x<32;x++){
            int tx=x>>3,tz=1;
            var tile=tiles.get(tz*4+tx);
            line[x]=graph.solver.temperature(tile.node+AdaptiveThermalTile.index(x&7,4,4));
        }
        return line;
    }

    private static double spread(double[] line){
        double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for(double v:line){min=Math.min(min,v);max=Math.max(max,v);}
        return max-min;
    }

    /** Decisive minimal check: does the mixing number change how fast heat spreads? */
    @Test public void mixingNumberChangesHeatSpread(){
        double slow=spreadTwoCells(3),fast=spreadTwoCells(40);
        System.out.println(String.format(Locale.ROOT,
            "TWOCELL mixing=3 air=%.2f C, mixing=40 air=%.2f C",slow,fast));
        assertTrue("stronger mixing must move heat faster: "+fast+" !> "+slow,fast>slow);
    }

    private static double spreadTwoCells(double mixing){
        var cold=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(-20)));
        var hot=new AdaptiveThermalTile(8,0,0,new TileTemperatureState(filled(-20)));
        double[] c=filled(AIR_CAPACITY),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];Arrays.fill(air,true);
        hot.temperatures.set(0,200);hot.configure(true,c,k,g,filled(1),filled(0));
        cold.configure(true,c,k,g,filled(1),filled(0));
        hot.fineMaterials(filled(1),air,3);cold.fineMaterials(filled(1),air,3);
        var graph=new AdaptiveThermalGraph(List.of(hot,cold));
        graph.setRoomAirMixing(mixing);
        for(int i=0;i<40;i++){graph.clearPower();graph.step(.5,-20);}
        return graph.solver.temperature(cold.node+AdaptiveThermalTile.index(0,0,0));
    }

    @Test public void uniformMixingFlattensATileHall(){
        double[] low=run(3),high=run(40);
        System.out.println(String.format(Locale.ROOT,
                "32-block hall: mixing=3 spread=%.2f C, mixing=40 spread=%.2f C "
                +"(left end %.2f, right end %.2f)",
                spread(low),spread(high),high[0],high[31]));
        assertTrue("stronger mixing must flatten the hall",spread(high)<spread(low));
    }

    @Test public void noEightBlockStepsAtTileBorders(){
        double[] line=run(40);
        // A step at a tile border would show as a jump between blocks 7-8, 15-16, 23-24.
        for(int border:new int[]{8,16,24}){
            double step=Math.abs(line[border]-line[border-1]);
            double typical=Math.abs(line[20]-line[19])+0.05;
            assertTrue("step at block "+border+" was "+step+", typical "+typical,step<typical*4);
        }
    }
}
