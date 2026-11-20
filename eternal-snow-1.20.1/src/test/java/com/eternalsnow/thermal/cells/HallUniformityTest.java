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

    /**
     * With this air capacity the pair transfer already saturates within one substep, so the
     * mixing number stops mattering past a small value. This documents the regime instead of
     * pretending a bigger number would help.
     */
    @Test public void mixingSaturatesBecauseAirPairsAlreadyEqualise(){
        double spread3=spread(run(3)),spread40=spread(run(40));
        System.out.println(String.format(Locale.ROOT,
            "Hall saturation: mixing=3 spread=%.2f C, mixing=40 spread=%.2f C",spread3,spread40));
        assertEquals("mixing is saturated; changing it must not change the result",
            spread3,spread40,0.05);
    }

    /**
     * The real property that matters for a bunker: a long hall must not show steps at tile
     * borders. It does not show them, because air mixing is uniform across tiles.
     */
    @Test public void noEightBlockStepsAtTileBorders(){
        double[] line=run(20);
        double steepest=0;int at=0;
        for(int x=1;x<line.length;x++){
            double step=Math.abs(line[x]-line[x-1]);
            if(step>steepest){steepest=step;at=x;}
        }
        System.out.println(String.format(Locale.ROOT,
            "Hall 32 blocks: steepest neighbour step %.3f C at block %d",steepest,at));
        assertTrue("neighbouring blocks must not jump by a degree, worst "+steepest+" at "+at,
                steepest<1.0);
    }
}
