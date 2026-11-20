package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: eight torches in a basement gave +150 C while it was -2.2 C outside, and the
 * ground around outdoor torches stayed at +3 C.
 *
 * <p>The mechanism: a solid block below the surface had <em>no weather coupling at all</em>
 * ({@code g = 0}), so the rock around a cellar was a heat reservoir that never leaked. Air got
 * its only sink from open sky cells, and the ground next to a torch simply stored the heat.
 *
 * <p>These tests pin both halves of the fix: buried ground must always have a (weak) path to the
 * weather, and it must not keep heat that no longer has a source.
 */
public class GroundAnchorTest {

    private static final double AIR_CAPACITY = 0.02;
    private static final double TORCH_POWER = 70;
    private static final int W = 16, H = 8, D = 16;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}
    private static int localIndex(int x,int y,int z){return AdaptiveThermalTile.index(x&7,y,z&7);}
    private static int tileIndex(int x,int z){return (x>>3)+2*(z>>3);}

    /**
     * A cellar cut into rock: floor and walls are undisturbed stone, the ceiling is what the
     * player built. Rock is deep, so it is only weakly coupled — this is the geometry from the
     * report.
     */
    private static List<AdaptiveThermalTile> cellar(boolean anchorGround){
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int tz=0;tz<D/8;tz++)for(int tx=0;tx<W/8;tx++){
            var tile=new AdaptiveThermalTile(tx*8,0,tz*8,new TileTemperatureState(filled(-2.2)));
            double[] cap=filled(1),k=filled(1),g=filled(0),anchor=new double[512];
            boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=tx*8+x,gz=tz*8+z;
                int i=AdaptiveThermalTile.index(x,y,z);
                boolean edge=gx==0||gz==0||gx==W-1||gz==D-1;
                boolean rock=y==0||edge;
                if(y==H-1){cap[i]=100;k[i]=5;g[i]=1;}                 // player ceiling: to the sky
                else if(rock){
                    cap[i]=100;k[i]=8;
                    // Weak coupling: this is what weatherCoupling now returns for deep stone.
                    g[i]=0.02*8;
                    if(anchorGround)anchor[i]=WorldCellThermal.groundTimeSeconds(40);
                }
                else{air[i]=true;cap[i]=AIR_CAPACITY;}
            }
            tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            tile.anchor=anchor;
            list.add(tile);
        }
        return list;
    }

    /** @return {mean air, peak air, mean rock} after 30 simulated minutes with 8 torches. */
    private static double[] run(boolean anchorGround){
        var tiles=cellar(anchorGround);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int[][] spots={{3,3},{11,3},{3,11},{11,11},{7,7},{6,6},{9,9},{4,8}};
        for(int step=0;step<3600;step++){
            graph.clearPower();
            for(int[] s:spots)graph.addPower(tiles.get(tileIndex(s[0],s[1])),localIndex(s[0],1,s[1]),TORCH_POWER);
            graph.step(.5,-2.2);
        }
        double airSum=0,rockSum=0,peak=-1e9;int airN=0,rockN=0;
        for(var t:tiles)for(int i=0;i<512;i++){
            boolean isAir=t.air!=null&&t.air[i];
            double v=t.temperatures.get(i);
            if(isAir){airSum+=v;airN++;peak=Math.max(peak,v);}
            else if(t.anchor!=null&&t.anchor[i]>0){rockSum+=v;rockN++;}
        }
        return new double[]{airSum/airN,peak,rockN==0?0:rockSum/rockN};
    }

    @Test public void buriedGroundAlwaysHasAPathToTheWeather(){
        // Surface at y 100, block at y 40: 60 blocks down. It must not be perfectly sealed.
        double deep=WorldCellThermal.weatherCoupling(false,false,40,100,8.0);
        double shallow=WorldCellThermal.weatherCoupling(false,false,95,100,8.0);
        System.out.println(String.format(Locale.ROOT,
            "Weather coupling: deep stone=%.3f, shallow stone=%.3f",deep,shallow));
        assertTrue("deep ground must leak something, got "+deep,deep>0);
        assertTrue("deeper ground must leak less: "+deep+" vs "+shallow,deep<shallow);
    }

    @Test public void airUnderTheOpenSkyStillFollowsTheWeather(){
        assertEquals("open-sky air follows the weather",1.0,
                WorldCellThermal.weatherCoupling(true,true,70,64,1.0),1e-9);
        assertEquals("air under a roof does not",0.0,
                WorldCellThermal.weatherCoupling(true,false,40,64,1.0),1e-9);
    }

    @Test public void torchesInACellarDoNotRunAway(){
        double[] r=run(true);
        System.out.println(String.format(Locale.ROOT,
            "Cellar, 8 torches: air mean=%.1f C, peak=%.1f C, rock=%.1f C",r[0],r[1],r[2]));
        assertTrue("a cellar must not reach +150 C, peak was "+r[1],r[1]<60);
        assertTrue("it must still be warmer than outside, mean "+r[0],r[0]>-2.2);
        assertTrue("the rock must not be cooked either, rock "+r[2],r[2]<20);
    }

    @Test public void withoutTheAnchorTheGroundOverheats(){
        // Documents why the anchor exists: sealed ground accumulates everything the fire makes.
        double[] sealedGround=run(false);
        double[] anchored=run(true);
        System.out.println(String.format(Locale.ROOT,
            "Rock temperature: sealed=%.1f C, anchored=%.1f C",sealedGround[2],anchored[2]));
        assertTrue("the anchor must cool the ground, sealed "+sealedGround[2]
                +" vs anchored "+anchored[2],anchored[2]<=sealedGround[2]+0.01);
    }
}
