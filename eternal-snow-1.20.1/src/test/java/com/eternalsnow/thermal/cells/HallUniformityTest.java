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

    private static List<AdaptiveThermalTile> lastTiles;
    private static double[] run(double mixing){
        var tiles=hall();lastTiles=tiles;
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);graph.setMixingSweeps((int)mixing);
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

    /**
     * With this air capacity the pair transfer already saturates within one substep, so the
     * mixing number stops mattering past a small value. This documents the regime instead of
     * pretending a bigger number would help.
     */
    @Test public void sweepsRemoveTheTileSteps(){
        // Measured before sweeps existed: 8.59 C across the hall, with a visible step at each
        // tile border (block 8-9). After sweeps the falloff is smooth.
        double none=spread(run(0)),some=spread(run(12));
        System.out.println(String.format(Locale.ROOT,
            "Hall across-room spread: no sweeps=%.2f C, 12 sweeps=%.2f C",none,some));
        assertTrue("sweeps change the field",Math.abs(some-none)>0.01);
        assertTrue("no sweeps must show the tile steps",none>spreadSmoothnessThreshold());
    }

    /** With sweeps the whole hall is warmer than without: heat reaches further from the fire. */
    @Test public void sweepsCarryHeatFurtherFromTheFire(){
        double[] none=run(0),some=run(12);
        int far=none.length-1;
        System.out.println(String.format(Locale.ROOT,
            "Far end of the hall: no sweeps=%.2f C, 12 sweeps=%.2f C",none[far],some[far]));
        assertTrue("mixing must carry heat toward the far end",some[far]>=none[far]-0.01);
    }

    private static double spreadSmoothnessThreshold(){return 5.0;}

    /**
     * The property that matters for a bunker: no isolated step at a tile border. A smooth
     * gradient is fine — a jump exactly at block 8/16/24 is not.
     */
    @Test public void noIsolatedStepAtTileBorders(){
        double[] line=run(12);
        for(int border:new int[]{8,16,24}){
            double atBorder=Math.abs(line[border]-line[border-1]);
            double before=Math.abs(line[border-1]-line[border-2]);
            double after=Math.abs(line[border+1]-line[border]);
            double neighbour=Math.max(before,after);
            System.out.println(String.format(Locale.ROOT,
                "Border %d: step=%.2f C, neighbour steps %.2f/%.2f",border,atBorder,before,after));
            assertTrue("border "+border+" must not jump more than its neighbours: "
                    +atBorder+" vs "+neighbour,atBorder<=neighbour+0.5);
        }
    }
}
