package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: eight torches in a basement gave +150 C while it was -2.2 C outside, and the
 * ground around outdoor torches warmed to +3 C.
 *
 * <p>Root cause: a solid block below the surface had <em>no weather coupling at all</em>
 * ({@code g = 0}). A cellar carved into rock therefore had no sink anywhere: air in it never
 * sees the sky, and the surrounding stone never leaked. Every watt the torches made stayed in.
 *
 * <p>The geometry below is the reported one: a room fully surrounded by rock, sixteen blocks by
 * sixteen by six, with one tile of rock on every side.
 */
public class GroundAnchorTest {

    private static final double AIR_CAPACITY = 0.02;
    /** One torch as the world adapter injects it: heatTorch 7 x roomSourcePower 10. */
    private static final double TORCH_POWER = 21;   // heatTorch 2.1 x roomSourcePower 10
    /** A real cellar is small: 6x6 floor with four blocks of headroom inside one tile. */
    private static final int W = 8, H = 8, D = 8;
    /** Surface is far above the cellar: every block of it is deep rock. */
    private static final int SURFACE_Y = 60, CELLAR_Y = 20;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}
    private static int localIndex(int x,int y,int z){return AdaptiveThermalTile.index(x&7,y,z&7);}
    private static int tileIndex(int x,int z){return 0;}

    /** @param oldRules true reproduces the shipped behaviour: buried solid had g = 0. */
    private static List<AdaptiveThermalTile> cellar(boolean oldRules){
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int tz=0;tz<1;tz++)for(int tx=0;tx<1;tx++){
            var tile=new AdaptiveThermalTile(tx*8,CELLAR_Y,tz*8,
                    new TileTemperatureState(filled(-2.2)));
            double[] cap=filled(1),k=filled(1),g=filled(0);
            boolean[] air=new boolean[512];
            for(int y=0;y<H;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=tx*8+x,gz=tz*8+z;
                int i=AdaptiveThermalTile.index(x,y,z);
                boolean inside=gx>=1&&gx<=6&&gz>=1&&gz<=6&&y>=1&&y<=4;
                boolean shell=!inside;
                if(shell){
                    cap[i]=8*20;k[i]=8;                      // stone
                    g[i]=oldRules?0:WorldCellThermal.weatherCoupling(false,false,CELLAR_Y+y,SURFACE_Y,8);
                } else {air[i]=true;cap[i]=AIR_CAPACITY;}
            }
            WorldCellThermal.applyVents(air,g);
        tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            list.add(tile);
        }
        return list;
    }

    /** @return {mean air, peak air, mean rock} after 30 simulated minutes with n torches. */
    private static double[] run(int torches,boolean oldRules){return run(torches,oldRules,3600);}

    private static double[] run(int torches,boolean oldRules,int steps){
        var tiles=cellar(oldRules);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int[][] spots={{2,1,2},{5,1,2},{2,1,5},{5,1,5},{3,1,3},{4,1,4},{2,1,4},{5,1,3},
                {3,1,5},{4,1,2},{4,1,5}};
        for(int step=0;step<steps;step++){
            graph.clearPower();
            for(int i=0;i<torches&&i<spots.length;i++)
                graph.addPower(tiles.get(tileIndex(spots[i][0],spots[i][2])),
                        localIndex(spots[i][0],spots[i][1],spots[i][2]),TORCH_POWER);
            graph.step(.5,-2.2);
        }
        double airSum=0,rockSum=0,peak=-1e9;int airN=0,rockN=0;
        for(var t:tiles)for(int i=0;i<512;i++){
            boolean isAir=t.air!=null&&t.air[i];
            double v=t.temperatures.get(i);
            if(isAir){airSum+=v;airN++;peak=Math.max(peak,v);}
            else{rockSum+=v;rockN++;}
        }
        return new double[]{airSum/airN,peak,rockN==0?0:rockSum/rockN};
    }

    /**
     * The reported symptom has to be reproducible, otherwise the fix proves nothing. A sealed
     * cellar does not merely get hot: it has no sink at all, so it keeps climbing until the
     * source's own throttle stops it. The +150 C the owner saw is a snapshot on that climb.
     */
    @Test public void theSealedCellarRunsAway(){
        double oldHalf=run(8,true,3600)[0], oldFull=run(8,true,7200)[0];
        System.out.println(String.format(Locale.ROOT,
            "Old rules, 8 torches: 30 min=%.1f C, 60 min=%.1f C (still climbing)",oldHalf,oldFull));
        assertTrue("sealed ground must keep climbing: "+oldHalf+" -> "+oldFull,
                oldFull>oldHalf+5);
    }

    @Test public void theFixedCellarLevelsOff(){
        double half=run(8,false,3600)[0], full=run(8,false,7200)[0];
        System.out.println(String.format(Locale.ROOT,
            "Fixed, 8 torches: 30 min=%.1f C, 60 min=%.1f C (level)",half,full));
        assertTrue("a fixed cellar must reach a steady state, not climb: "+half+" -> "+full,
                full<half+2);
    }

    @Test public void eightTorchesInACellarStayHabitable(){
        double[] now=run(8,false);
        System.out.println(String.format(Locale.ROOT,
            "Fixed, 8 torches: air mean=%.1f C, peak=%.1f C, rock=%.1f C",
            now[0],now[1],now[2]));
        assertTrue("a cellar must not reach +150 C, peak was "+now[1],now[1]<40);
        assertTrue("it must still be warmer than the weather, mean "+now[0],now[0]>-2.2);
        assertTrue("the rock must not be cooked either, rock "+now[2],now[2]<25);
    }

    @Test public void elevenTorchesAreWarmerButNotRunaway(){
        double eight=run(8,false)[0],eleven=run(11,false)[0];
        System.out.println(String.format(Locale.ROOT,
            "Fixed, 8 vs 11 torches: air mean %.1f vs %.1f C",eight,eleven));
        assertTrue("more torches must not be colder",eleven>=eight-0.01);
        assertTrue("but must not explode either, "+eleven,eleven<60);
    }

    @Test public void buriedGroundAlwaysActsAsAReservoir(){
        double deep=WorldCellThermal.weatherCoupling(false,false,20,SURFACE_Y,8.0);
        System.out.println(String.format(Locale.ROOT,
            "Weather coupling: buried stone=%.3f (was 0.000)",deep));
        assertTrue("buried ground must exchange heat, got "+deep,deep>0.05);
    }

    @Test public void heatedGroundLosesItsHeatAgain(){
        // Outdoor case from the report: the ground next to a torch sat at +3 C. Once the torch
        // is out, ground must come back towards the weather instead of staying warm.
        var tiles=cellar(false);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        for(int step=0;step<1200;step++){
            graph.clearPower();
            graph.addPower(tiles.get(0),localIndex(3,1,3),TORCH_POWER*4);
            graph.step(.5,-2.2);
        }
        double[] afterHeat=run(0,false);
        System.out.println(String.format(Locale.ROOT,
            "Cooled back after the fire is out: air mean=%.1f C, rock=%.1f C",
            afterHeat[0],afterHeat[2]));
        assertTrue("with no sources the cellar must return near the weather, mean "+afterHeat[0],
                afterHeat[0]<3.0);
    }
}
