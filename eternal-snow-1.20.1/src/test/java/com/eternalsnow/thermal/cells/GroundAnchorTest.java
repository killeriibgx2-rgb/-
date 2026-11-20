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
    private static final double TORCH_POWER = 70;
    private static final int W = 16, H = 8, D = 16;
    /** Surface is far above the cellar: every block of it is deep rock. */
    private static final int SURFACE_Y = 60, CELLAR_Y = 20;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}
    private static int localIndex(int x,int y,int z){return AdaptiveThermalTile.index(x&7,y,z&7);}
    private static int tileIndex(int x,int z){return (x>>3)+2*(z>>3);}

    /**
     * @param oldRules true reproduces the shipped behaviour (buried solid g = 0, no anchor)
     * @param anchor   apply the ground anchor
     */
    private static List<AdaptiveThermalTile> cellar(boolean oldRules,boolean anchor){
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int tz=0;tz<D/8;tz++)for(int tx=0;tx<W/8;tx++){
            var tile=new AdaptiveThermalTile(tx*8,CELLAR_Y,tz*8,
                    new TileTemperatureState(filled(-2.2)));
            double[] cap=filled(1),k=filled(1),g=filled(0),anchorArr=new double[512];
            boolean[] air=new boolean[512];
            for(int y=0;y<H;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=tx*8+x,gz=tz*8+z;
                int i=AdaptiveThermalTile.index(x,y,z);
                boolean shell=y==0||y==H-1||gx==0||gz==0||gx==W-1||gz==D-1;
                if(shell){
                    cap[i]=8*20;k[i]=8;                      // stone
                    int depth=SURFACE_Y-(CELLAR_Y+y);
                    g[i]=oldRules?0:WorldCellThermal.weatherCoupling(false,false,CELLAR_Y+y,SURFACE_Y,8);
                    if(anchor&&!oldRules)anchorArr[i]=WorldCellThermal.groundTimeSeconds(depth);
                } else {air[i]=true;cap[i]=AIR_CAPACITY;}
            }
            tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            tile.anchor=anchorArr;
            list.add(tile);
        }
        return list;
    }

    /** @return {mean air, peak air, mean rock} after 30 simulated minutes with n torches. */
    private static double[] run(int torches,boolean oldRules,boolean anchor){
        var tiles=cellar(oldRules,anchor);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int[][] spots={{3,1,3},{11,1,3},{3,1,11},{11,1,11},{7,1,7},{6,1,6},{9,1,9},{4,1,8},
                {12,1,12},{5,1,5},{8,1,2}};
        for(int step=0;step<3600;step++){
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
            else if(t.anchor!=null&&!isAir){rockSum+=v;rockN++;}
        }
        return new double[]{airSum/airN,peak,rockN==0?0:rockSum/rockN};
    }

    /** The reported symptom has to be reproducible, otherwise the fix proves nothing. */
    @Test public void theSealedCellarReproducesTheReport(){
        double[] old=run(8,true,false);
        System.out.println(String.format(Locale.ROOT,
            "Old rules, 8 torches: air mean=%.1f C, peak=%.1f C, rock=%.1f C",
            old[0],old[1],old[2]));
        assertTrue("sealed ground must run away, that is the bug we are fixing: peak "
                + old[1], old[1] > 80);
    }

    @Test public void eightTorchesInACellarStayHabitable(){
        double[] now=run(8,false,true);
        System.out.println(String.format(Locale.ROOT,
            "Fixed, 8 torches: air mean=%.1f C, peak=%.1f C, rock=%.1f C",
            now[0],now[1],now[2]));
        assertTrue("a cellar must not reach +150 C, peak was "+now[1],now[1]<60);
        assertTrue("it must still be warmer than the weather, mean "+now[0],now[0]>-2.2);
        assertTrue("the rock must not be cooked either, rock "+now[2],now[2]<25);
    }

    @Test public void elevenTorchesAreWarmerButNotRunaway(){
        double eight=run(8,false,true)[0],eleven=run(11,false,true)[0];
        System.out.println(String.format(Locale.ROOT,
            "Fixed, 8 vs 11 torches: air mean %.1f vs %.1f C",eight,eleven));
        assertTrue("more torches must not be colder",eleven>=eight-0.01);
        assertTrue("but must not explode either, "+eleven,eleven<120);
    }

    @Test public void buriedGroundAlwaysHasAPathToTheWeather(){
        double deep=WorldCellThermal.weatherCoupling(false,false,20,SURFACE_Y,8.0);
        double shallow=WorldCellThermal.weatherCoupling(false,false,SURFACE_Y-4,SURFACE_Y,8.0);
        System.out.println(String.format(Locale.ROOT,
            "Weather coupling: deep stone=%.3f, near-surface stone=%.3f",deep,shallow));
        assertTrue("deep ground must leak something, got "+deep,deep>0);
        assertTrue("deeper must leak less: "+deep+" vs "+shallow,deep<shallow);
    }

    @Test public void heatedGroundLosesItsHeatAgain(){
        // Outdoor case from the report: the ground next to a torch sat at +3 C. Once the torch
        // is out, ground must come back towards the weather instead of staying warm.
        var tiles=cellar(false,true);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        for(int step=0;step<1200;step++){
            graph.clearPower();
            graph.addPower(tiles.get(tileIndex(7,7)),localIndex(7,1,7),TORCH_POWER*4);
            graph.step(.5,-2.2);
        }
        double[] afterHeat=run(0,false,true);
        System.out.println(String.format(Locale.ROOT,
            "Cooled back after the fire is out: air mean=%.1f C, rock=%.1f C",
            afterHeat[0],afterHeat[2]));
        assertTrue("with no sources the cellar must return near the weather, mean "+afterHeat[0],
                afterHeat[0]<3.0);
    }
}
