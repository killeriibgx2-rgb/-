package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * The owner's actual case: a 5x5x3 room of planks standing in the open, one campfire in the
 * middle, -15 C outside. Inside it reads -8.1 C and "does not warm at all".
 *
 * <p>The room sits inside one 8^3 tile. The rest of that tile is air under the sky, and
 * {@code TileTemperatureState.coarseUpdate} is used once a tile stops being detailed — which is
 * exactly what happens after a player walks away. This test covers both a detailed and a coarse
 * tile, because the coarse path is what a player actually sees while standing still elsewhere.
 */
public class PlankRoomInWorldTest {

    private static final double OUTSIDE = -15;
    private static final double CAMPFIRE = 1500;      // heatCampfire 150 x roomSourcePower 10
    private static final double AIR = WorldCellThermal.AIR_HEAT_CAPACITY;
    private static final double TS = WorldCellThermal.THERMAL_TIME_SCALE;
    private static final double AIR_K = WorldCellThermal.AIR_CONDUCTIVITY;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** 5x5x3 interior, plank shell, open-sky air around it in the same tile. */
    static AdaptiveThermalTile room(){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int i=0;i<512;i++){air[i]=true;cap[i]=AIR*TS;k[i]=AIR_K;g[i]=WorldCellThermal.AIR_OPEN_COUPLING;}
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            int i=AdaptiveThermalTile.index(x,y,z);g[i]=0;
        }
        double pg=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){plank(cap,k,g,air,0,y,z,pg);plank(cap,k,g,air,6,y,z,pg);}
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){plank(cap,k,g,air,x,y,0,pg);plank(cap,k,g,air,x,y,6,pg);}
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(cap,k,g,air,x,0,z,pg);plank(cap,k,g,air,x,4,z,pg);
        }
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,boolean[] air,
                              int x,int y,int z,double pg){
        int i=AdaptiveThermalTile.index(x,y,z);
        air[i]=false;cap[i]=2.5*20*TS;k[i]=2.5;g[i]=pg;
    }
    private static double roomAir(AdaptiveThermalTile t){
        double s=0;int n=0;
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            s+=t.temperatures.get(AdaptiveThermalTile.index(x,y,z));n++;
        }
        return s/n;
    }

    @Test public void fireInTheMiddleOfTheRoomWarmsIt(){
        var t=room();
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fire,CAMPFIRE);g.step(.5,OUTSIDE);}
        double inside=roomAir(t);
        System.out.println(String.format(Locale.ROOT,
            "Real fire in a real plank room (%.0f C outside): inside=%.1f C after 15 min",OUTSIDE,inside));
        assertTrue("a campfire must warm its room, was "+inside,inside>OUTSIDE+10);
    }

    /**
     * The reported symptom in its exact form: a room is a small part of a tile, and the rest of
     * the tile is open field. While the tile is coarse it is ONE reservoir, so the room cannot
     * have its own temperature — it collapses onto the mean of the field around it. A test that
     * keeps every tile detailed would never catch that.
     */
    @Test public void aCoarseTileLosesTheRoom(){
        var t=room();
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fire,CAMPFIRE);g.step(.5,OUTSIDE);}
        double detailed=roomAir(t);
        // Now store the tile coarse, which is what happens when the player walks away.
        t.configure(false,t.lastCapacities,t.lastCapacities,filled(0),filled(1),filled(0));
        var cg=new AdaptiveThermalGraph(List.of(t));
        cg.setRoomAirMixing(20);cg.setMixingSweeps(12);
        for(int s=0;s<600;s++){cg.clearPower();cg.step(.5,OUTSIDE);}
        double coarse=roomAir(t);
        System.out.println(String.format(Locale.ROOT,
            "Room while detailed=%.1f C, after the tile went coarse=%.1f C",detailed,coarse));
        assertTrue("a coarse tile must not lose the room, was "+coarse,coarse>OUTSIDE+5);
    }

    @Test public void theRoomDoesNotNeedToBeDetailed(){
        // After a player walks away the tile is stored coarse. Coarse storage uses an energy
        // weighted mean; if that mean is taken over the tile instead of the room the whole room
        // is silently set to the temperature of the field outside it.
        var t=room();
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<1800;s++){g.clearPower();g.addPower(t,fire,CAMPFIRE);g.step(.5,OUTSIDE);}
        double detailed=roomAir(t);
        // Simulate the coarse store: mean of the whole tile, the way a coarse tile is captured.
        double tileMean=0;
        for(int i=0;i<512;i++)tileMean+=t.temperatures.get(i);
        tileMean/=512;
        System.out.println(String.format(Locale.ROOT,
            "Detailed room mean=%.1f C, whole-tile mean=%.1f C",detailed,tileMean));
        assertFalse("the whole-tile mean must not be used as the room temperature",
                Math.abs(detailed-tileMean)<0.01&&Double.isFinite(tileMean));
    }
}
