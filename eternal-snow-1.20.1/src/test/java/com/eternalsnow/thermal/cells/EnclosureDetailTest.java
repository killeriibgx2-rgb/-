package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A room is small next to the tile that contains it. An 8^3 tile is 512 blocks; a 5x5x3 room is
 * 75 of them and the other 437 are open field. When a tile is stored coarse it holds ONE
 * temperature, so the room cannot exist in it: the whole tile is the mean of the field around the
 * house, which is the weather. The player then sees a campfire that does not warm anything, and a
 * hole in the ceiling that reads as outdoor temperature.
 *
 * <p>So a tile that contains enclosed air must keep its detail.
 */
public class EnclosureDetailTest {

    private static final double OUTSIDE = -15;
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    private static AdaptiveThermalTile tile(boolean enclosed,boolean detailed){
        var t=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=WorldCellThermal.AIR_HEAT_CAPACITY;k[i]=0.02;g[i]=1;}
        if(enclosed)for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            int i=AdaptiveThermalTile.index(x,y,z);g[i]=0;
        }
        t.configure(detailed,cap,k,g,filled(1),filled(0));
        t.fineMaterials(filled(1),air,3);
        return t;
    }

    @Test public void aTileWithARoomKeepsItsDetail(){
        assertTrue("a tile containing enclosed air must stay detailed",
                tile(true,true).keepDetailed());
    }

    @Test public void aTileOfOpenFieldMayBeCoarse(){
        assertFalse("a tile of open field has no micro-climate to lose",
                tile(false,true).keepDetailed());
    }

    /**
     * The protection is that such a tile is never demoted in the first place. A coarse tile
     * applies {@code coarseUpdate}, which shrinks sub-tile contrast towards the tile mean by
     * design, so no amount of pattern preservation can save a room once the tile is coarse.
     */
    @Test public void aTileWithARoomIsNeverDemoted(){
        var room=tile(true,true);
        assertTrue("the world adapter must keep this tile detailed",room.keepDetailed());
        var field=tile(false,true);
        assertFalse("open field may be demoted freely",field.keepDetailed());
        // And a tile that is already coarse reports that it must be promoted again.
        var staleCoarse=tile(true,false);
        assertFalse("a coarse tile cannot claim protection until it is rebuilt",
                staleCoarse.keepDetailed());
    }
}
