package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Making solid ground an infinite reservoir is the right physics for rock, but it must not turn
 * a player's house into a freezer: a stone floor then sits at the outside temperature and pulls
 * heat out of the room. A house has to stay warmer than the weather, and a fire must still work.
 */
public class FloorHeatTest {

    private static final double AIR_CAPACITY = 0.02;
    private static final double CAMPFIRE_POWER = 680;   // heatCampfire 68 x roomSourcePower 10

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /**
     * A single-tile house: stone floor at ground level, plank walls, plank roof, air inside.
     *
     * @param floorG weather coupling of the floor; 0.42 is what the ground rule now gives stone
     */
    private static List<AdaptiveThermalTile> house(double floorG){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(-15)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            boolean edge=x==0||z==0||x==7||z==7;
            if(y==0){cap[i]=8*20;k[i]=8;g[i]=floorG;}                 // stone floor
            else if(y==7){cap[i]=2.5*20;k[i]=0.24;g[i]=0.24;}          // plank roof
            else if(edge){cap[i]=2.5*20;k[i]=0.24;g[i]=0.24;}          // plank walls
            else{air[i]=true;cap[i]=AIR_CAPACITY;}
        }
        WorldCellThermal.applyVents(air,g);
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return List.of(tile);
    }

    /** @return {mean air, peak air} after 20 simulated minutes. */
    private static double[] run(double floorG,double power){
        var tiles=house(floorG);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        for(int step=0;step<2400;step++){
            graph.clearPower();
            if(power>0)graph.addPower(tiles.get(0),AdaptiveThermalTile.index(4,1,4),power);
            graph.step(.5,-15);
        }
        double sum=0,peak=-1e9;int n=0;
        for(var t:tiles)for(int i=0;i<512;i++){
            if(t.air==null||!t.air[i])continue;
            double v=t.temperatures.get(i);sum+=v;n++;peak=Math.max(peak,v);
        }
        return new double[]{sum/n,peak};
    }

    @Test public void anUnheatedHouseFollowsTheWeather(){
        // A stone floor reaches the ground temperature and the air above it follows. The point
        // of this test is the other direction: the house must never end up colder than outside,
        // which a thermostat-like ground anchor could do if it were misapplied to air.
        double[] none=run(WorldCellThermal.weatherCoupling(false,false,0,0,8.0),0);
        System.out.println(String.format(Locale.ROOT,
            "Stone floor at -15 C outside, no fire: air mean=%.2f C",none[0]));
        assertTrue("an unheated house must not fall below the weather, mean "+none[0],none[0]>-15.01);
    }

    @Test public void aCampfireStillWarmsAStoneFlooredHouse(){
        double[] fire=run(WorldCellThermal.weatherCoupling(false,false,0,0,8.0),CAMPFIRE_POWER);
        System.out.println(String.format(Locale.ROOT,
            "Stone floor, one campfire: air mean=%.1f C, peak=%.1f C",fire[0],fire[1]));
        assertTrue("one campfire must warm the house above freezing, mean "+fire[0],fire[0]>0);
    }

    @Test public void anInsulatedFloorIsNotWorseThanAStoneOne(){
        double stone=run(0.42,CAMPFIRE_POWER)[0], sealed=run(0.0,CAMPFIRE_POWER)[0];
        System.out.println(String.format(Locale.ROOT,
            "One campfire: stone floor=%.1f C, sealed floor=%.1f C",stone,sealed));
        assertTrue("a floor that leaks heat must not be warmer than a sealed one",stone<=sealed+0.01);
    }
}
